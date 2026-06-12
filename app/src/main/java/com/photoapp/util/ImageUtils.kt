package com.photoapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {

    /**
     * Load a bitmap from a URI with optional downsampling for memory efficiency.
     */
    suspend fun loadBitmap(
        context: Context,
        uri: Uri,
        maxWidth: Int = 2048,
        maxHeight: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // First, get dimensions without loading
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false

            // Load the bitmap
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Apply a ColorMatrix to a bitmap.
     */
    fun applyColorMatrix(source: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * Create a ColorMatrix for brightness, contrast, and saturation adjustments.
     */
    fun createAdjustmentMatrix(
        brightness: Float = 0f,   // -100 to 100
        contrast: Float = 0f,     // -100 to 100
        saturation: Float = 0f,   // -100 to 100
        warmth: Float = 0f        // -100 to 100
    ): ColorMatrix {
        val cm = ColorMatrix()

        // Brightness
        if (brightness != 0f) {
            val brightnessMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, brightness,
                0f, 1f, 0f, 0f, brightness,
                0f, 0f, 1f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(brightnessMatrix)
        }

        // Contrast
        if (contrast != 0f) {
            val scale = (100f + contrast) / 100f
            val translate = (-(0.5f * scale) + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastMatrix)
        }

        // Saturation
        if (saturation != 0f) {
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(1f + saturation / 100f)
            cm.postConcat(satMatrix)
        }

        // Warmth (adjust red and blue channels)
        if (warmth != 0f) {
            val warmthScale = warmth * 0.5f
            val warmthMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, warmthScale,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, -warmthScale,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(warmthMatrix)
        }

        return cm
    }

    /**
     * Rotate a bitmap by the given degrees.
     */
    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Flip a bitmap horizontally or vertically.
     */
    fun flipBitmap(source: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) {
                preScale(-1f, 1f)
            } else {
                preScale(1f, -1f)
            }
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Crop a bitmap to the given rectangle.
     */
    fun cropBitmap(source: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        val safeX = x.coerceIn(0, source.width - 1)
        val safeY = y.coerceIn(0, source.height - 1)
        val safeWidth = width.coerceAtMost(source.width - safeX)
        val safeHeight = height.coerceAtMost(source.height - safeY)
        return Bitmap.createBitmap(source, safeX, safeY, safeWidth, safeHeight)
    }

    /**
     * Save a bitmap to a file in the Pictures directory.
     */
    suspend fun saveBitmap(
        bitmap: Bitmap,
        filename: String? = null,
        quality: Int = 95
    ): File? = withContext(Dispatchers.IO) {
        try {
            val name = filename ?: "PhotoApp_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            }.jpg"

            val picturesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val appDir = File(picturesDir, "PhotoApp")
            if (!appDir.exists()) appDir.mkdirs()

            val file = File(appDir, name)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}

/**
 * Predefined photo filters using ColorMatrix.
 */
enum class PhotoFilter(val displayName: String, val matrix: ColorMatrix) {
    NONE("Original", ColorMatrix()),
    BW("B&W", ColorMatrix().apply { setSaturation(0f) }),
    SEPIA("Sepia", ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))),
    VINTAGE("Vintage", ColorMatrix(floatArrayOf(
        0.9f, 0.5f, 0.1f, 0f, 20f,
        0.3f, 0.8f, 0.1f, 0f, 10f,
        0.2f, 0.3f, 0.5f, 0f, 30f,
        0f, 0f, 0f, 1f, 0f
    ))),
    COOL("Cool", ColorMatrix(floatArrayOf(
        0.9f, 0f, 0f, 0f, -10f,
        0f, 0.95f, 0f, 0f, 0f,
        0f, 0f, 1.1f, 0f, 20f,
        0f, 0f, 0f, 1f, 0f
    ))),
    WARM("Warm", ColorMatrix(floatArrayOf(
        1.1f, 0f, 0f, 0f, 15f,
        0f, 1.0f, 0f, 0f, 5f,
        0f, 0f, 0.9f, 0f, -10f,
        0f, 0f, 0f, 1f, 0f
    ))),
    VIVID("Vivid", ColorMatrix().apply {
        setSaturation(1.8f)
        val contrastMatrix = ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, -25f,
            0f, 1.2f, 0f, 0f, -25f,
            0f, 0f, 1.2f, 0f, -25f,
            0f, 0f, 0f, 1f, 0f
        ))
        postConcat(contrastMatrix)
    }),
    DRAMATIC("Dramatic", ColorMatrix().apply {
        setSaturation(0.6f)
        val contrastMatrix = ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, -60f,
            0f, 1.5f, 0f, 0f, -60f,
            0f, 0f, 1.5f, 0f, -60f,
            0f, 0f, 0f, 1f, 0f
        ))
        postConcat(contrastMatrix)
    }),
    FADE("Fade", ColorMatrix(floatArrayOf(
        1f, 0f, 0f, 0f, 40f,
        0f, 1f, 0f, 0f, 40f,
        0f, 0f, 1f, 0f, 40f,
        0f, 0f, 0f, 0.9f, 0f
    )).apply { setSaturation(0.7f) })
}
