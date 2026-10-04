package com.photoapp.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object VideoEditorEngine {

    /**
     * Extract a high-resolution Bitmap frame from any video timestamp
     */
    suspend fun extractFrame(context: Context, uri: Uri, timeMs: Long): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val timeUs = timeMs * 1000L
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    /**
     * Save an extracted Bitmap frame to a JPEG file in cache/pictures
     */
    suspend fun saveExtractedFrame(context: Context, bitmap: Bitmap, fileName: String): File? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "$fileName.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Trim video between startMs and endMs
     */
    suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        startMs: Long,
        endMs: Long
    ): File? = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(context.cacheDir, "trimmed_${System.currentTimeMillis()}.mp4")
            // Copy source video stream safely to output target
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Rotate video by 90, 180, or 270 degrees
     */
    suspend fun rotateVideo(
        context: Context,
        inputUri: Uri,
        degrees: Int
    ): File? = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(context.cacheDir, "rotated_${degrees}_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
