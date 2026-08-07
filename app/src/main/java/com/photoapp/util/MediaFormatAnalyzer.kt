package com.photoapp.util

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface

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
        val deviceModel: String? = null,
        val megapixels: String? = null
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
        var deviceModel: String? = null
        val mp = (width * height) / 1_000_000.0
        val megapixels = if (mp >= 0.1) "${Math.round(mp)} MP" else null

        // 1. Resolution classification
        val maxDim = maxOf(width, height)
        val resolutionTag = when {
            maxDim >= 7680 -> "8K Ultra"
            maxDim >= 3840 -> "4K Ultra HD"
            maxDim >= 1920 -> "1080p Full HD"
            maxDim >= 1280 -> "720p HD"
            else -> null
        }

        if (!isVideo) {
            // 2. Image characteristics
            val mp = (width * height) / 1_000_000.0
            if (mp >= 1.0) {
                val mpFormatted = String.format("%.1f MP", mp)
                extraTags.add(mpFormatted)
                if (mp >= 12.0) {
                    extraTags.add("High-Res")
                }
            }

            // Check if HDR/Wide Color Gamut (Display P3, BT.2020) via EXIF
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
                    val colorSpace = exif.getAttributeInt(
                        ExifInterface.TAG_COLOR_SPACE,
                        ExifInterface.COLOR_SPACE_UNCALIBRATED
                    )
                    
                    // Standard color space tag: 1 = sRGB, 2 = Adobe RGB, 65535 = Uncalibrated
                    // Some HDR/Wide color images report uncalibrated or specific profile tags
                    if (colorSpace == ExifInterface.COLOR_SPACE_UNCALIBRATED) {
                        // Wide color / custom ICC Profile used (typical of HDR/P3 images)
                        // Verify extension or format
                        val mimeType = context.contentResolver.getType(uri) ?: ""
                        if (mimeType.contains("heic") || mimeType.contains("heif") || mimeType.contains("avif")) {
                            hdrTag = "HDR (HEIF)"
                        }
                    }

                    // Extract Camera / Exposure details
                    val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                    if (fNumber > 0.0) {
                        aperture = "f/${fNumber}"
                    }
                    
                    val exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                    if (exposureTime > 0.0) {
                        shutterSpeed = if (exposureTime >= 1.0) {
                            "${String.format("%.1f", exposureTime)} S"
                        } else {
                            val reciprocal = Math.round(1.0 / exposureTime)
                            "1/$reciprocal S"
                        }
                    }
                    
                    val bias = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, 0.0)
                    exposureBias = "${if (bias >= 0.0) "+" else ""}${String.format("%.1f", bias)} EV"
                    
                    val isoVal = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
                    if (isoVal > 0) {
                        iso = "ISO $isoVal"
                    }
                    
                    val focal35 = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0.0)
                    if (focal35 > 0.0) {
                        focalLength = "${focal35.toInt()} MM"
                    } else {
                        val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                        if (focal > 0.0) {
                            focalLength = if (focal % 1.0 == 0.0) "${focal.toInt()} MM" else "${String.format(java.util.Locale.US, "%.1f", focal)} MM"
                        }
                    }
                    
                    val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                    val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
                    deviceModel = when {
                        !make.isNullOrEmpty() && !model.isNullOrEmpty() -> {
                            if (model.contains(make, ignoreCase = true)) model else "$make $model"
                        }
                        !model.isNullOrEmpty() -> model
                        !make.isNullOrEmpty() -> make
                        else -> null
                    }?.uppercase()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // 3. Video characteristics (HDR, Dolby Vision, Frame Rate)

            try {
                val uri = Uri.parse(uriString)
                val extractor = MediaExtractor()
                extractor.setDataSource(context, uri, null)
                val trackCount = extractor.trackCount
                
                for (i in 0 until trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    
                    if (mime.startsWith("video/")) {
                        // Check track type for Dolby Vision
                        if (mime.contains("dolby-vision")) {
                            hdrTag = "Dolby Vision"
                        }

                        // Check color transfer (PQ / HLG)
                        if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                            val colorTransfer = format.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
                            // 6 = PQ (SMPTE ST 2084), 7 = HLG (Hybrid Log Gamma)
                            if (colorTransfer == MediaFormat.COLOR_TRANSFER_ST2084 || colorTransfer == 6) {
                                if (hdrTag == null) hdrTag = "HDR10"
                            } else if (colorTransfer == MediaFormat.COLOR_TRANSFER_HLG || colorTransfer == 7) {
                                if (hdrTag == null) hdrTag = "HLG HDR"
                            }
                        }

                        // Check wide color gamut (BT.2020)
                        if (format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) {
                            val colorStandard = format.getInteger(MediaFormat.KEY_COLOR_STANDARD)
                            if (colorStandard == MediaFormat.COLOR_STANDARD_BT2020 || colorStandard == 6) {
                                extraTags.add("BT.2020 WCG")
                            }
                        }

                        // Check framerate
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
                // Fallback to MediaMetadataRetriever if extractor fails
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
            deviceModel = deviceModel,
            megapixels = megapixels
        )
    }
}
