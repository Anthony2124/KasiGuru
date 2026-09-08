package com.kasiguru.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object ImageCompressor {

    private const val MAX_DIMENSION = 1024
    private const val COMPRESS_QUALITY = 75

    // firestore.rules caps `photoBase64` at 700,000 characters, and it measures the
    // stored string — the whole data URI, prefix included. Nothing here checked the
    // result before, so an oversized photo was accepted at attach time and only
    // rejected on submit, by the server, as a generic permission-denied. The user saw
    // their report fail with nothing they could act on. Sitting a little under the cap
    // leaves room for the rule to move without turning this into an off-by-a-few-bytes
    // failure.
    private const val MAX_ENCODED_CHARS = 690_000

    // Tried in order until the encoding fits. Below roughly 35 the JPEG artefacts get
    // bad enough that a physically smaller image is the better trade, which is what the
    // scale ladder below is for.
    private val QUALITY_LADDER = intArrayOf(COMPRESS_QUALITY, 60, 45, 35)

    // Each pass takes a quarter off the longest side; three passes bring a 1024px photo
    // to about 432px, still perfectly legible for a bug report screenshot.
    private const val SCALE_STEPS = 3
    private const val SCALE_FACTOR = 0.75f

    /**
     * Compresses the image at [imageUri] and returns a Base64 data URI string
     * formatted as `data:image/jpeg;base64,<encoded>`.
     */
    suspend fun compressToBase64(context: Context, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds to determine downsampling
            var input: InputStream? = contentResolver.openInputStream(imageUri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Invalid image dimensions"))
            }

            // Calculate sample size
            var inSampleSize = 1
            val maxSide = max(srcWidth, srcHeight)
            if (maxSide > MAX_DIMENSION) {
                inSampleSize = maxSide / MAX_DIMENSION
            }

            // 2. Decode actual scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            input = contentResolver.openInputStream(imageUri)
            val decodedBitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
            input?.close()

            if (decodedBitmap == null) {
                return@withContext Result.failure(IllegalStateException("Could not decode image from URI"))
            }

            // 3. Fix EXIF orientation if needed
            val rotatedBitmap = try {
                contentResolver.openInputStream(imageUri)?.use { exifInput ->
                    val exif = android.media.ExifInterface(exifInput)
                    val orientation = exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                    val rotationDegrees = when (orientation) {
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                    if (rotationDegrees != 0f) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees) }
                        Bitmap.createBitmap(
                            decodedBitmap, 0, 0,
                            decodedBitmap.width, decodedBitmap.height,
                            matrix, true
                        )
                    } else {
                        decodedBitmap
                    }
                } ?: decodedBitmap
            } catch (_: Throwable) {
                decodedBitmap
            }

            // 4. Exact scale down if still larger than max dimension
            val finalWidth = rotatedBitmap.width
            val finalHeight = rotatedBitmap.height
            val scale = min(1f, MAX_DIMENSION.toFloat() / max(finalWidth, finalHeight))
            val finalBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    rotatedBitmap,
                    (finalWidth * scale).toInt(),
                    (finalHeight * scale).toInt(),
                    true
                )
            } else {
                rotatedBitmap
            }

            // 5. Compress and encode, dropping quality first and only then dimensions,
            //    until the result is small enough for Firestore to accept. A photo from
            //    a modern phone camera can still exceed the cap at 1024px/q75, so the
            //    single fixed pass this used to do was a coin flip.
            var candidate = finalBitmap
            var encoded: String? = null
            var scaleStep = 0

            search@ while (true) {
                for (quality in QUALITY_LADDER) {
                    val dataUrl = encodeToDataUrl(candidate, quality)
                    if (dataUrl.length <= MAX_ENCODED_CHARS) {
                        encoded = dataUrl
                        break@search
                    }
                }
                if (scaleStep >= SCALE_STEPS) break
                scaleStep++
                candidate = Bitmap.createScaledBitmap(
                    candidate,
                    (candidate.width * SCALE_FACTOR).toInt().coerceAtLeast(1),
                    (candidate.height * SCALE_FACTOR).toInt().coerceAtLeast(1),
                    true
                )
            }

            if (encoded == null) {
                // Reported here rather than left to the server, so the user hears about
                // it while the picker is still the obvious next step.
                return@withContext Result.failure(
                    IllegalStateException(
                        "it is still too large after compression. Try a smaller photo, or a screenshot."
                    )
                )
            }

            Result.success(encoded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun encodeToDataUrl(bitmap: Bitmap, quality: Int): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64String"
    }
}
