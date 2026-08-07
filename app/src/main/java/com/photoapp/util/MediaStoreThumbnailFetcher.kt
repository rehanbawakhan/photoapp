package com.photoapp.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size as AndroidSize
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import java.io.File
import java.io.FileOutputStream

class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val size = options.size
        val widthDimension = size.width
        val heightDimension = size.height

        if (widthDimension !is Dimension.Pixels || heightDimension !is Dimension.Pixels) {
            // Fallback to default fetcher if size is not a fixed pixel value
            return null
        }

        val width = widthDimension.px
        val height = heightDimension.px

        // Only handle thumbnail-sized queries to avoid downsampling full-screen viewer loads
        if (width > 1024 || height > 1024) {
            return null
        }

        val id = try {
            data.lastPathSegment?.toLong()
        } catch (e: Exception) {
            null
        } ?: return null

        val thumbDir = File(context.cacheDir, "thumbnails")
        if (!thumbDir.exists()) {
            thumbDir.mkdirs()
        }
        val cacheFile = File(thumbDir, "${id}.jpg")

        // 1. Check local cache first
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val bitmapOptions = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val cachedBitmap = BitmapFactory.decodeFile(cacheFile.absolutePath, bitmapOptions)
                if (cachedBitmap != null) {
                    return DrawableResult(
                        drawable = BitmapDrawable(context.resources, cachedBitmap),
                        isSampled = true,
                        dataSource = DataSource.DISK
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fallback to generating and saving thumbnail
        return try {
            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(
                    data,
                    AndroidSize(width, height),
                    null
                )
            } else {
                // Fallback for API 26-28
                val isVideo = data.toString().contains("video")
                if (isVideo) {
                    @Suppress("DEPRECATION")
                    MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        id,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Thumbnails.getThumbnail(
                        context.contentResolver,
                        id,
                        MediaStore.Images.Thumbnails.MINI_KIND,
                        null
                    )
                }
            }

            if (bitmap != null) {
                // Save to local cache file asynchronously/synchronously
                try {
                    FileOutputStream(cacheFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null // Fallback to Coil's default fetcher if system load fails
        }
    }

    companion object {
        fun clearCacheForId(context: Context, id: Long) {
            try {
                val cacheFile = File(File(context.cacheDir, "thumbnails"), "${id}.jpg")
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val isMediaStoreUri = data.scheme == ContentResolver.SCHEME_CONTENT &&
                    (data.authority == MediaStore.AUTHORITY || data.authority == "media")
            if (isMediaStoreUri) {
                return MediaStoreThumbnailFetcher(context, data, options)
            }
            return null
        }
    }
}
