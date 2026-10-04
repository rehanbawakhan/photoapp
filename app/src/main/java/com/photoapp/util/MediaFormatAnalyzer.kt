package com.photoapp.util

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.File

object MediaFormatAnalyzer {

    data class MediaInfo(
        val resolutionTag: String?,
        val hdrTag: String?,
        val extraTags: List<String> = emptyList(),
        val aperture: String? = null,
        val shutterSpeed: String? = null,
        val exposureBias: String? = null,
        val iso: String? = null,
        val focalLength: String? = null,
        val cameraMake: String? = null,
        val cameraModel: String? = null,
        val lensModel: String? = null,
        val deviceModel: String? = null,
        val megapixels: String? = null,
        val actualWidth: Int = 0,
        val actualHeight: Int = 0,
        val fileSizeFormatted: String? = null,
        val dateTakenFormatted: String? = null,
        val dateModifiedFormatted: String? = null,
        val mimeType: String? = null
    )

    fun analyze(
        context: Context,
        uriString: String,
        path: String,
        isVideo: Boolean,
        width: Int,
        height: Int
    ): MediaInfo {
        var hdrTag: String? = null
        val extraTags = mutableListOf<String>()
        var aperture: String? = null
        var shutterSpeed: String? = null
        var exposureBias: String? = "0.0 EV"
        var iso: String? = null
        var focalLength: String? = null
        var cameraMake: String? = null
        var cameraModel: String? = null
        var lensModel: String? = null
        var deviceModel: String? = null
        var actualWidth = width
        var actualHeight = height
        var fileSizeFormatted: String? = null
        var dateTakenFormatted: String? = null
        var dateModifiedFormatted: String? = null
        var mimeType: String? = context.contentResolver.getType(Uri.parse(uriString))

        // Get file size & modified date if file exists
        if (path.isNotEmpty()) {
            val file = File(path)
            if (file.exists()) {
                val sizeBytes = file.length()
                val kb = sizeBytes / 1024.0
                val mb = kb / 1024.0
                fileSizeFormatted = when {
                    mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                    kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
                    else -> "$sizeBytes B"
                }
                if (file.lastModified() > 0) {
                    dateModifiedFormatted = DateUtils.formatDateTime(file.lastModified())
                }
            }
        }

        val mp = (actualWidth * actualHeight) / 1_000_000.0
        val megapixels = if (mp >= 0.1) "${String.format(java.util.Locale.US, "%.1f", mp)} MP" else null

        // 1. Resolution classification
        val maxDim = maxOf(actualWidth, actualHeight)
        val resolutionTag = when {
            maxDim >= 7680 -> "8K Ultra"
            maxDim >= 3840 -> "4K Ultra HD"
            maxDim >= 1920 -> "1080p Full HD"
            maxDim >= 1280 -> "720p HD"
            else -> null
        }

        if (!isVideo) {
            // 2. Image characteristics & EXIF parsing
            if (mp >= 1.0) {
                val mpFormatted = String.format(java.util.Locale.US, "%.1f MP", mp)
                extraTags.add(mpFormatted)
                if (mp >= 12.0) {
                    extraTags.add("High-Res")
                }
            }

            try {
                val uri = Uri.parse(uriString)
                val photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        MediaStore.setRequireOriginal(uri)
                    } catch (e: Exception) {
                        uri
                    }
                } else {
                    uri
                }
                context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    
                    // Width / Height from EXIF
                    val exifW = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                    val exifH = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                    if (exifW > 0 && exifH > 0) {
                        actualWidth = exifW
                        actualHeight = exifH
                    }

                    val colorSpace = exif.getAttributeInt(
                        ExifInterface.TAG_COLOR_SPACE,
                        ExifInterface.COLOR_SPACE_UNCALIBRATED
                    )
                    
                    if (colorSpace == ExifInterface.COLOR_SPACE_UNCALIBRATED) {
                        val type = mimeType ?: ""
                        if (type.contains("heic") || type.contains("heif") || type.contains("avif")) {
                            hdrTag = "HDR (HEIF)"
                        }
                    }

                    // Extract Camera / Exposure details
                    val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                    if (fNumber > 0.0) {
                        aperture = "f/${String.format(java.util.Locale.US, "%.1f", fNumber)}"
                    }
                    
                    val exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                    if (exposureTime > 0.0) {
                        shutterSpeed = if (exposureTime >= 1.0) {
                            "${String.format(java.util.Locale.US, "%.1f", exposureTime)} S"
                        } else {
                            val reciprocal = Math.round(1.0 / exposureTime)
                            "1/$reciprocal S"
                        }
                    }
                    
                    val bias = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, 0.0)
                    exposureBias = "${if (bias >= 0.0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", bias)} EV"
                    
                    var isoVal = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
                    if (isoVal == 0) {
                        isoVal = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0)
                    }
                    if (isoVal > 0) {
                        iso = "ISO $isoVal"
                    }
                    
                    val focal35 = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0.0)
                    val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                    if (focal35 > 0.0) {
                        focalLength = "${focal35.toInt()} MM"
                    } else if (focal > 0.0) {
                        focalLength = if (focal % 1.0 == 0.0) "${focal.toInt()} MM" else "${String.format(java.util.Locale.US, "%.1f", focal)} MM"
                    }

                    cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
                    
                    val rawLens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim()
                        ?: exif.getAttribute(ExifInterface.TAG_LENS_MAKE)?.trim()
                    if (!rawLens.isNullOrEmpty()) {
                        lensModel = rawLens
                    }

                    deviceModel = when {
                        !cameraMake.isNullOrEmpty() && !cameraModel.isNullOrEmpty() -> {
                            if (cameraModel!!.contains(cameraMake!!, ignoreCase = true)) cameraModel else "$cameraMake $cameraModel"
                        }
                        !cameraModel.isNullOrEmpty() -> cameraModel
                        !cameraMake.isNullOrEmpty() -> cameraMake
                        else -> null
                    }

                    // Date taken from EXIF
                    val dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                    if (!dateTimeOriginal.isNullOrEmpty()) {
                        try {
                            val sdf = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.getDefault())
                            val parsedDate = sdf.parse(dateTimeOriginal)
                            if (parsedDate != null) {
                                dateTakenFormatted = DateUtils.formatDateTime(parsedDate.time)
                            }
                        } catch (e: Exception) {
                            // ignore parse error
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // 3. Video characteristics
            try {
                val uri = Uri.parse(uriString)
                val extractor = MediaExtractor()
                extractor.setDataSource(context, uri, null)
                val trackCount = extractor.trackCount
                
                for (i in 0 until trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    
                    if (mime.startsWith("video/")) {
                        mimeType = mime
                        if (mime.contains("dolby-vision")) {
                            hdrTag = "Dolby Vision"
                        }

                        if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                            val colorTransfer = format.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
                            if (colorTransfer == MediaFormat.COLOR_TRANSFER_ST2084 || colorTransfer == 6) {
                                if (hdrTag == null) hdrTag = "HDR10"
                            } else if (colorTransfer == MediaFormat.COLOR_TRANSFER_HLG || colorTransfer == 7) {
                                if (hdrTag == null) hdrTag = "HLG HDR"
                            }
                        }

                        if (format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) {
                            val colorStandard = format.getInteger(MediaFormat.KEY_COLOR_STANDARD)
                            if (colorStandard == MediaFormat.COLOR_STANDARD_BT2020 || colorStandard == 6) {
                                extraTags.add("BT.2020 WCG")
                            }
                        }

                        if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            val frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                            if (frameRate >= 120) {
                                extraTags.add("Slow-Mo ($frameRate fps)")
                            } else if (frameRate >= 60) {
                                extraTags.add("$frameRate fps")
                            }
                        }
                        break
                    }
                }
                extractor.release()
            } catch (e: Exception) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, Uri.parse(uriString))
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val colorTransfer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)
                        if (colorTransfer == "6") {
                            hdrTag = "HDR10"
                        } else if (colorTransfer == "7") {
                            hdrTag = "HLG HDR"
                        }
                    }

                    val frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    if (frameRateStr != null) {
                        val fr = frameRateStr.toFloatOrNull()?.toInt() ?: 0
                        if (fr >= 120) {
                            extraTags.add("Slow-Mo ($fr fps)")
                        } else if (fr >= 60) {
                            extraTags.add("$fr fps")
                        }
                    }
                    retriever.release()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        return MediaInfo(
            resolutionTag = resolutionTag,
            hdrTag = hdrTag,
            extraTags = extraTags,
            aperture = aperture,
            shutterSpeed = shutterSpeed,
            exposureBias = exposureBias,
            iso = iso,
            focalLength = focalLength,
            cameraMake = cameraMake,
            cameraModel = cameraModel,
            lensModel = lensModel,
            deviceModel = deviceModel,
            megapixels = megapixels,
            actualWidth = actualWidth,
            actualHeight = actualHeight,
            fileSizeFormatted = fileSizeFormatted,
            dateTakenFormatted = dateTakenFormatted,
            dateModifiedFormatted = dateModifiedFormatted,
            mimeType = mimeType
        )
    }
}
