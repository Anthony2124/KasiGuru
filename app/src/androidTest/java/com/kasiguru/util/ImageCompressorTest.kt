package com.kasiguru.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.abs

class ImageCompressorTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun jpegOrientationsPreserveDimensionsAndCornerOrder() = runBlocking {
        val cases = listOf(
            ExifInterface.ORIENTATION_NORMAL to listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW),
            ExifInterface.ORIENTATION_ROTATE_90 to listOf(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN),
            ExifInterface.ORIENTATION_ROTATE_180 to listOf(Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED),
            ExifInterface.ORIENTATION_ROTATE_270 to listOf(Color.GREEN, Color.YELLOW, Color.RED, Color.BLUE)
        )
        for ((orientation, corners) in cases) {
            val file = fixture(Bitmap.CompressFormat.JPEG)
            try {
                ExifInterface(file).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                    saveAttributes()
                }
                val result = decode(ImageCompressor.compressToBase64(context, Uri.fromFile(file)).getOrThrow())
                val sideways = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == ExifInterface.ORIENTATION_ROTATE_270
                assertEquals(if (sideways) 120 else 240, result.width)
                assertEquals(if (sideways) 240 else 120, result.height)
                assertCorners(result, corners)
                result.recycle()
            } finally {
                file.delete()
            }
        }
    }

    @Test
    fun pngWithoutOrientationStillCompresses() = runBlocking {
        val file = fixture(Bitmap.CompressFormat.PNG)
        try {
            val result = decode(ImageCompressor.compressToBase64(context, Uri.fromFile(file)).getOrThrow())
            assertEquals(240, result.width)
            assertEquals(120, result.height)
            assertCorners(result, listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW))
            result.recycle()
        } finally {
            file.delete()
        }
    }

    @Test
    fun largeImageKeepsDimensionAndEncodedSizeLimits() = runBlocking {
        val file = fixture(Bitmap.CompressFormat.JPEG, 2048, 1024)
        try {
            val data = ImageCompressor.compressToBase64(context, Uri.fromFile(file)).getOrThrow()
            val result = decode(data)
            assertEquals(1024, result.width)
            assertEquals(512, result.height)
            assertTrue(data.length <= 690_000)
            result.recycle()
        } finally {
            file.delete()
        }
    }

    @Test
    fun invalidImageReturnsFailure() = runBlocking {
        val file = File.createTempFile("image-compressor-test-", ".bin", context.cacheDir)
        try {
            file.writeText("not an image")
            assertTrue(ImageCompressor.compressToBase64(context, Uri.fromFile(file)).isFailure)
        } finally {
            file.delete()
        }
    }

    private fun fixture(format: Bitmap.CompressFormat, width: Int = 240, height: Int = 120): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        val corners = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
        for (row in 0..1) for (column in 0..1) {
            paint.color = corners[row * 2 + column]
            canvas.drawRect(column * width / 2f, row * height / 2f,
                (column + 1) * width / 2f, (row + 1) * height / 2f, paint)
        }
        val file = File.createTempFile("image-compressor-test-", ".img", context.cacheDir)
        file.outputStream().use { assertTrue(bitmap.compress(format, 100, it)) }
        bitmap.recycle()
        return file
    }

    private fun decode(data: String): Bitmap {
        assertTrue(data.startsWith("data:image/jpeg;base64,"))
        val bytes = Base64.decode(data.substringAfter(','), Base64.DEFAULT)
        return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
    }

    private fun assertCorners(bitmap: Bitmap, expected: List<Int>) {
        for (row in 0..1) for (column in 0..1) {
            val actual = bitmap.getPixel((column * 2 + 1) * bitmap.width / 4,
                (row * 2 + 1) * bitmap.height / 4)
            val color = expected[row * 2 + column]
            assertTrue(abs(Color.red(actual) - Color.red(color)) < 40)
            assertTrue(abs(Color.green(actual) - Color.green(color)) < 40)
            assertTrue(abs(Color.blue(actual) - Color.blue(color)) < 40)
        }
    }
}
