package com.photoapp.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

/**
 * On-device AI editing engine powered by Google ML Kit Subject Segmentation.
 * All processing runs locally — no API keys, no cloud calls.
 */
object AiEditingEngine {

    private val segmenter by lazy {
        SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder()
                .enableForegroundConfidenceMask()
                .build()
        )
    }

    /**
     * Request ML Kit module download if not already installed.
     */
    fun ensureModelDownloaded(context: android.content.Context, onReady: () -> Unit) {
        val moduleInstallClient = ModuleInstall.getClient(context)
        val request = ModuleInstallRequest.newBuilder()
            .addApi(SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().build()))
            .build()
        moduleInstallClient.installModules(request)
            .addOnSuccessListener { onReady() }
            .addOnFailureListener { onReady() } // proceed anyway, segmentation may still work
    }

    /**
     * Segment the subject from the background.
     * Returns a confidence mask as a FloatBuffer (0.0 = background, 1.0 = subject).
     * The mask dimensions match the input bitmap dimensions.
     */
    private suspend fun getSubjectMask(bitmap: Bitmap): FloatBuffer =
        suspendCancellableCoroutine { cont ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            segmenter.process(inputImage)
                .addOnSuccessListener { result ->
                    val mask = result.foregroundConfidenceMask
                    if (mask != null) {
                        cont.resume(mask)
                    } else {
                        cont.resumeWithException(Exception("No subject detected"))
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    // ──────────────────────────────────────────────
    // 1. BACKGROUND BLUR
    // ──────────────────────────────────────────────

    /**
     * Blur the background while keeping the subject sharp.
     * @param blurRadius 1–25, higher = more blur
     */
    suspend fun blurBackground(bitmap: Bitmap, blurRadius: Int = 15): Bitmap =
        withContext(Dispatchers.Default) {
            val mask = getSubjectMask(bitmap)

            // Create blurred version using stack blur (no RenderScript needed)
            val blurred = stackBlur(bitmap.copy(Bitmap.Config.ARGB_8888, true), blurRadius)

            // Composite: for each pixel, blend between blurred (bg) and original (subject)
            val width = bitmap.width
            val height = bitmap.height
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val origPixels = IntArray(width * height)
            val blurPixels = IntArray(width * height)
            bitmap.getPixels(origPixels, 0, width, 0, 0, width, height)
            blurred.getPixels(blurPixels, 0, width, 0, 0, width, height)

            val resultPixels = IntArray(width * height)
            mask.rewind()

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val i = y * width + x

                    val confidence = if (i < mask.limit()) {
                        mask.get(i).coerceIn(0f, 1f)
                    } else 0f

                    val origColor = origPixels[i]
                    val blurColor = blurPixels[i]

                    val r = lerp(Color.red(blurColor), Color.red(origColor), confidence)
                    val g = lerp(Color.green(blurColor), Color.green(origColor), confidence)
                    val b = lerp(Color.blue(blurColor), Color.blue(origColor), confidence)
                    val a = Color.alpha(origColor)

                    resultPixels[i] = Color.argb(a, r, g, b)
                }
            }

            result.setPixels(resultPixels, 0, width, 0, 0, width, height)
            result
        }

    // ──────────────────────────────────────────────
    // 2. BACKGROUND REMOVE
    // ──────────────────────────────────────────────

    enum class BackgroundFill { TRANSPARENT, WHITE, BLACK }

    /**
     * Remove the background and replace with specified fill.
     */
    suspend fun removeBackground(
        bitmap: Bitmap,
        fill: BackgroundFill = BackgroundFill.TRANSPARENT
    ): Bitmap = withContext(Dispatchers.Default) {
        val mask = getSubjectMask(bitmap)

        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val origPixels = IntArray(width * height)
        bitmap.getPixels(origPixels, 0, width, 0, 0, width, height)

        val bgColor = when (fill) {
            BackgroundFill.TRANSPARENT -> Color.TRANSPARENT
            BackgroundFill.WHITE -> Color.WHITE
            BackgroundFill.BLACK -> Color.BLACK
        }

        val resultPixels = IntArray(width * height)
        mask.rewind()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x

                val confidence = if (i < mask.limit()) {
                    mask.get(i).coerceIn(0f, 1f)
                } else 0f

                if (confidence > 0.5f) {
                    // Subject pixel — keep with smooth edge alpha
                    val origColor = origPixels[i]
                    val alpha = (confidence * 255).toInt().coerceIn(0, 255)
                    resultPixels[i] = if (fill == BackgroundFill.TRANSPARENT) {
                        Color.argb(alpha, Color.red(origColor), Color.green(origColor), Color.blue(origColor))
                    } else {
                        origColor
                    }
                } else {
                    // Background pixel — replace
                    if (fill == BackgroundFill.TRANSPARENT) {
                        resultPixels[i] = Color.TRANSPARENT
                    } else {
                        // Blend smoothly at edges
                        val origColor = origPixels[i]
                        val r = lerp(Color.red(bgColor), Color.red(origColor), confidence)
                        val g = lerp(Color.green(bgColor), Color.green(origColor), confidence)
                        val b = lerp(Color.blue(bgColor), Color.blue(origColor), confidence)
                        resultPixels[i] = Color.argb(255, r, g, b)
                    }
                }
            }
        }

        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        result
    }

    // ──────────────────────────────────────────────
    // 3. AI AUTO-ENHANCE
    // ──────────────────────────────────────────────

    data class EnhanceParams(
        val brightness: Float,
        val contrast: Float,
        val saturation: Float,
        val warmth: Float
    )

    /**
     * Analyze image histogram and calculate optimal enhancement parameters.
     */
    suspend fun analyzeForAutoEnhance(bitmap: Bitmap): EnhanceParams =
        withContext(Dispatchers.Default) {
            val width = bitmap.width
            val height = bitmap.height
            // Sample pixels for speed (every 4th pixel)
            val step = 4
            var totalBrightness = 0.0
            var totalSaturation = 0.0
            var totalR = 0.0
            var totalG = 0.0
            var totalB = 0.0
            var count = 0

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            for (y in 0 until height step step) {
                for (x in 0 until width step step) {
                    val color = pixels[y * width + x]
                    val r = Color.red(color)
                    val g = Color.green(color)
                    val b = Color.blue(color)

                    // Perceived brightness (ITU-R BT.601)
                    val brightness = 0.299 * r + 0.587 * g + 0.114 * b
                    totalBrightness += brightness

                    // Saturation estimate
                    val maxC = maxOf(r, g, b).toFloat()
                    val minC = minOf(r, g, b).toFloat()
                    val sat = if (maxC > 0) (maxC - minC) / maxC else 0f
                    totalSaturation += sat

                    totalR += r
                    totalG += g
                    totalB += b
                    count++
                }
            }

            if (count == 0) return@withContext EnhanceParams(0f, 0f, 0f, 0f)

            val avgBrightness = totalBrightness / count
            val avgSaturation = totalSaturation / count
            val avgR = totalR / count
            val avgB = totalB / count

            // Target: brightness ~128, saturation ~0.4, neutral white balance
            val brightnessAdj = ((128.0 - avgBrightness) * 0.4).toFloat().coerceIn(-50f, 50f)
            val contrastAdj = if (avgBrightness in 80.0..170.0) 12f else 5f // boost if flat
            val saturationAdj = ((0.45 - avgSaturation) * 80).toFloat().coerceIn(-20f, 35f)
            val warmthAdj = ((avgB - avgR) * 0.08).toFloat().coerceIn(-20f, 20f)

            EnhanceParams(brightnessAdj, contrastAdj, saturationAdj, warmthAdj)
        }

    /**
     * Apply auto-enhancement directly to a bitmap.
     */
    suspend fun autoEnhance(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val params = analyzeForAutoEnhance(bitmap)
        val matrix = ImageUtils.createAdjustmentMatrix(
            brightness = params.brightness,
            contrast = params.contrast,
            saturation = params.saturation,
            warmth = params.warmth
        )
        ImageUtils.applyColorMatrix(bitmap, matrix)
    }

    // ──────────────────────────────────────────────
    // 4. OBJECT HIGHLIGHT (Color Pop)
    // ──────────────────────────────────────────────

    /**
     * Keep subject in full color, desaturate the background to B&W.
     */
    suspend fun highlightSubject(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val mask = getSubjectMask(bitmap)

        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val origPixels = IntArray(width * height)
        bitmap.getPixels(origPixels, 0, width, 0, 0, width, height)

        val resultPixels = IntArray(width * height)
        mask.rewind()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x

                val confidence = if (i < mask.limit()) {
                    mask.get(i).coerceIn(0f, 1f)
                } else 0f

                val origColor = origPixels[i]
                val r = Color.red(origColor)
                val g = Color.green(origColor)
                val b = Color.blue(origColor)

                // Grayscale version using luminance weights
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)

                // Blend between grayscale (bg) and original color (subject)
                val finalR = lerp(gray, r, confidence)
                val finalG = lerp(gray, g, confidence)
                val finalB = lerp(gray, b, confidence)

                resultPixels[i] = Color.argb(Color.alpha(origColor), finalR, finalG, finalB)
            }
        }

        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        result
    }

    // ──────────────────────────────────────────────
    // UTILITIES
    // ──────────────────────────────────────────────

    private fun lerp(a: Int, b: Int, t: Float): Int {
        return (a + (b - a) * t).toInt().coerceIn(0, 255)
    }

    /**
     * Stack blur implementation — works without RenderScript.
     * Based on Mario Klingemann's stack blur algorithm.
     */
    private fun stackBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 25)
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = r + r + 1

        val rArr = IntArray(wh)
        val gArr = IntArray(wh)
        val bArr = IntArray(wh)
        var rsum: Int; var gsum: Int; var bsum: Int
        var rinsum: Int; var ginsum: Int; var binsum: Int
        var routsum: Int; var goutsum: Int; var boutsum: Int

        val vmin = IntArray(maxOf(w, h))
        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (i in dv.indices) dv[i] = i / divsum

        var yi = 0
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = r + 1

        for (y in 0 until h) {
            rinsum = 0; ginsum = 0; binsum = 0
            routsum = 0; goutsum = 0; boutsum = 0
            rsum = 0; gsum = 0; bsum = 0

            for (i in -r..r) {
                val p = pixels[yi + min(wm, maxOf(i, 0))]
                sir = stack[i + r]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                }
            }
            stackpointer = r

            for (x in 0 until w) {
                rArr[yi] = dv[rsum]; gArr[yi] = dv[gsum]; bArr[yi] = dv[bsum]

                rsum -= routsum; gsum -= goutsum; bsum -= boutsum

                stackstart = stackpointer - r + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]

                if (y == 0) vmin[x] = min(x + r + 1, wm)
                val p = pixels[vmin[x] + (y * w)]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]

                yi++
            }
        }

        for (x in 0 until w) {
            rinsum = 0; ginsum = 0; binsum = 0
            routsum = 0; goutsum = 0; boutsum = 0
            rsum = 0; gsum = 0; bsum = 0

            var yp = -r * w
            for (i in -r..r) {
                yi = maxOf(0, yp) + x
                sir = stack[i + r]
                sir[0] = rArr[yi]; sir[1] = gArr[yi]; sir[2] = bArr[yi]
                rbs = r1 - kotlin.math.abs(i)
                rsum += rArr[yi] * rbs; gsum += gArr[yi] * rbs; bsum += bArr[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                }
                if (i < hm) yp += w
            }

            yi = x
            stackpointer = r
            for (y in 0 until h) {
                pixels[yi] = (0xff000000.toInt() and pixels[yi]) or
                        (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum; gsum -= goutsum; bsum -= boutsum

                stackstart = stackpointer - r + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]

                if (x == 0) vmin[y] = min(y + r1, hm) * w
                val p = x + vmin[y]
                sir[0] = rArr[p]; sir[1] = gArr[p]; sir[2] = bArr[p]
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]

                yi += w
            }
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }
}
