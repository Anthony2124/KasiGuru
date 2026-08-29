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

            // 5. Compress to JPEG bytes
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outputStream)
            val bytes = outputStream.toByteArray()

            // 6. Encode to Base64 data URL
            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUrl = "data:image/jpeg;base64,$base64String"

            Result.success(dataUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
