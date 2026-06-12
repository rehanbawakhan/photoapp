package com.photoapp.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
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

        return try {
            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(
                    data,
                    AndroidSize(width, height),
                    null
                )
            } else {
                // Fallback for API 26-28
                val id = try {
                    data.lastPathSegment?.toLong()
                } catch (e: Exception) {
                    null
                }
                if (id == null) return null

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
