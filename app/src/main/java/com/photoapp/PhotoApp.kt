package com.photoapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PhotoApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(com.photoapp.util.MediaStoreThumbnailFetcher.Factory(this@PhotoApp))
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.40) // Increased memory cache size to 40% of available RAM for smoother fast scrolling
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(1024 * 1024 * 1024) // Increased disk cache to 1GB to store more persistent thumbnails
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
