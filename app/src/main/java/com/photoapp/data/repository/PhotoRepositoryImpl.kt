package com.photoapp.data.repository

import android.content.Context
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.os.Environment
import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import com.photoapp.data.local.PhotoDao
import com.photoapp.data.local.entities.AlbumEntity
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.data.media.MediaStoreManager
import com.photoapp.util.MediaStoreThumbnailFetcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.exifinterface.media.ExifInterface

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao,
    private val mediaStoreManager: MediaStoreManager,
    @ApplicationContext private val context: Context
) : PhotoRepository {

    // ── Photos ──────────────────────────────────────────────────────────

    override fun getAllPhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getAllPhotos()
    }

    override suspend fun getPhotoById(id: Long): PhotoEntity? {
        return photoDao.getPhotoById(id)
    }

    override fun observePhotoById(id: Long): Flow<PhotoEntity?> {
        return photoDao.observePhotoById(id)
    }

    private fun getExifLocation(context: Context, uriString: String): Pair<Double, Double>? {
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
                val latLong = exif.latLong
                if (latLong != null && latLong.size >= 2) {
                    val lat = latLong[0]
                    val lng = latLong[1]
                    if (lat != 0.0 || lng != 0.0) {
                        return Pair(lat, lng)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getVideoLocation(context: Context, uriString: String): Pair<Double, Double>? {
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(uriString))
            val locationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_LOCATION)
            retriever.release()
            if (locationStr != null) {
                val regex = """([+-]\d+\.\d+)([+-]\d+\.\d+)""".toRegex()
                val match = regex.find(locationStr)
                if (match != null) {
                    val lat = match.groupValues[1].toDoubleOrNull()
                    val lng = match.groupValues[2].toDoubleOrNull()
                    if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                        return Pair(lat, lng)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override suspend fun syncPhotos() = withContext(Dispatchers.IO) {
        val mediaPhotos = mediaStoreManager.loadPhotos()
        val existingPhotos = photoDao.getAllPhotosList()

        // Create a map of existing photos for quick lookup
        val existingMap = existingPhotos.associateBy { it.id }
        
        val toInsert = mutableListOf<PhotoEntity>()
        val toDelete = mutableListOf<Long>()

        // Find new or modified photos
        for (mediaPhoto in mediaPhotos) {
            val existing = existingMap[mediaPhoto.id]
            if (existing == null) {
                // New photo
                var photoWithLocation = mediaPhoto
                if (photoWithLocation.latitude == null || photoWithLocation.longitude == null || 
                    (photoWithLocation.latitude == 0.0 && photoWithLocation.longitude == 0.0)) {
                    val isVideo = photoWithLocation.mimeType.startsWith("video/")
                    val loc = if (isVideo) getVideoLocation(context, photoWithLocation.uri) else getExifLocation(context, photoWithLocation.uri)
                    if (loc != null) {
                        photoWithLocation = photoWithLocation.copy(latitude = loc.first, longitude = loc.second)
                    }
                }
                toInsert.add(photoWithLocation)
            } else if (existing.dateModified != mediaPhoto.dateModified || 
                       existing.size != mediaPhoto.size || 
                       existing.path != mediaPhoto.path) {
                // Modified photo: copy favorite/deleted/hidden states
                var photoWithLocation = mediaPhoto
                if (photoWithLocation.latitude == null || photoWithLocation.longitude == null || 
                    (photoWithLocation.latitude == 0.0 && photoWithLocation.longitude == 0.0)) {
                    val isVideo = photoWithLocation.mimeType.startsWith("video/")
                    val loc = if (isVideo) getVideoLocation(context, photoWithLocation.uri) else getExifLocation(context, photoWithLocation.uri)
                    if (loc != null) {
                        photoWithLocation = photoWithLocation.copy(latitude = loc.first, longitude = loc.second)
                    }
                }
                toInsert.add(
                    photoWithLocation.copy(
                        isFavorite = existing.isFavorite,
                        isDeleted = existing.isDeleted,
                        dateDeleted = existing.dateDeleted,
                        isHidden = existing.isHidden
                    )
                )
            }
        }

        // Find deleted photos (exist in local DB but no longer in MediaStore)
        val mediaIds = mediaPhotos.map { it.id }.toSet()
        for (existing in existingPhotos) {
            if (existing.id !in mediaIds && !existing.isHidden) {
                toDelete.add(existing.id)
            }
        }

        // Perform DB updates only if changes exist
        if (toInsert.isNotEmpty()) {
            photoDao.insertPhotos(toInsert)
        }
        if (toDelete.isNotEmpty()) {
            photoDao.deletePhotosByIds(toDelete)
        }

        // Scan existing photos in the database that are missing location metadata and update them
        val missingLocationPhotos = existingPhotos.filter {
            !it.isHidden && !it.isDeleted && 
            (it.latitude == null || it.longitude == null || (it.latitude == 0.0 && it.longitude == 0.0))
        }
        if (missingLocationPhotos.isNotEmpty()) {
            val toUpdate = mutableListOf<PhotoEntity>()
            for (photo in missingLocationPhotos) {
                val isVideo = photo.mimeType.startsWith("video/")
                val loc = if (isVideo) getVideoLocation(context, photo.uri) else getExifLocation(context, photo.uri)
                if (loc != null) {
                    toUpdate.add(photo.copy(latitude = loc.first, longitude = loc.second))
                }
            }
            if (toUpdate.isNotEmpty()) {
                photoDao.insertPhotos(toUpdate)
            }
        }
    }

    // ── Favorites ───────────────────────────────────────────────────────

    override fun getFavoritePhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getFavoritePhotos()
    }

    override suspend fun toggleFavorite(id: Long) = withContext(Dispatchers.IO) {
        val photo = photoDao.getPhotoById(id) ?: return@withContext
        photoDao.setFavorite(id, !photo.isFavorite)
    }

    override suspend fun setFavoriteMultiple(ids: List<Long>) = withContext(Dispatchers.IO) {
        val photos = ids.mapNotNull { photoDao.getPhotoById(it) }
        if (photos.isEmpty()) return@withContext
        val allAreFavorites = photos.all { it.isFavorite }
        photoDao.setFavoriteMultiple(ids, !allAreFavorites)
    }

    // ── Trash ───────────────────────────────────────────────────────────

    override fun getTrashPhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getTrashPhotos()
    }

    override suspend fun moveToTrash(id: Long) = withContext(Dispatchers.IO) {
        photoDao.moveToTrash(id)
    }

    override suspend fun moveToTrashMultiple(ids: List<Long>) = withContext(Dispatchers.IO) {
        photoDao.moveToTrashMultiple(ids)
    }

    override suspend fun restoreFromTrash(id: Long) = withContext(Dispatchers.IO) {
        photoDao.restoreFromTrash(id)
    }

    override suspend fun restoreAllFromTrash() = withContext(Dispatchers.IO) {
        val trashPhotos = photoDao.getExpiredTrashPhotos(Long.MAX_VALUE)
        val ids = trashPhotos.map { it.id }
        photoDao.restoreAllFromTrash(ids)
    }

    override suspend fun permanentlyDelete(id: Long) = withContext(Dispatchers.IO) {
        val photo = photoDao.getPhotoById(id) ?: return@withContext
        // Delete from device storage
        mediaStoreManager.deletePhotoFromMediaStore(photo.contentUri)
        // Remove from database
        photoDao.deletePhoto(photo)
        // Clear thumbnail cache
        MediaStoreThumbnailFetcher.clearCacheForId(context, id)
    }

    override suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val trashPhotos = photoDao.getExpiredTrashPhotos(Long.MAX_VALUE)
        for (photo in trashPhotos) {
            mediaStoreManager.deletePhotoFromMediaStore(photo.contentUri)
            MediaStoreThumbnailFetcher.clearCacheForId(context, photo.id)
        }
        photoDao.emptyTrash()
    }

    override suspend fun cleanupExpiredTrash() = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val expiredPhotos = photoDao.getExpiredTrashPhotos(thirtyDaysAgo)
        for (photo in expiredPhotos) {
            mediaStoreManager.deletePhotoFromMediaStore(photo.contentUri)
            photoDao.deletePhoto(photo)
            MediaStoreThumbnailFetcher.clearCacheForId(context, photo.id)
        }
    }

    // ── Albums ──────────────────────────────────────────────────────────

    override fun getAllAlbums(): Flow<List<AlbumEntity>> {
        return photoDao.getAllAlbums()
    }

    override fun getPhotosByBucket(bucketId: String): Flow<List<PhotoEntity>> {
        return photoDao.getPhotosByBucket(bucketId)
    }

    override suspend fun syncAlbums() = withContext(Dispatchers.IO) {
        val albums = mediaStoreManager.loadAlbums()
        photoDao.deleteAutoAlbums()
        photoDao.insertAlbums(albums)
    }

    // ── Search ──────────────────────────────────────────────────────────

    override fun searchPhotos(query: String): Flow<List<PhotoEntity>> {
        return photoDao.searchPhotos(query)
    }

    // ── Share ───────────────────────────────────────────────────────────

    override suspend fun getShareUri(photoId: Long): Uri? {
        val photo = photoDao.getPhotoById(photoId)
        return photo?.contentUri
    }

    // ── Delete from device ──────────────────────────────────────────────

    override suspend fun deleteFromDevice(photoId: Long): Boolean {
        val photo = photoDao.getPhotoById(photoId) ?: return false
        val deleted = mediaStoreManager.deletePhotoFromMediaStore(photo.contentUri)
        if (deleted) {
            photoDao.deletePhoto(photo)
            MediaStoreThumbnailFetcher.clearCacheForId(context, photoId)
        }
        return deleted
    }

    override suspend fun getDeleteIntentSender(ids: List<Long>): android.content.IntentSender? {
        val photos = ids.mapNotNull { photoDao.getPhotoById(it) }
        if (photos.isEmpty()) return null
        val uris = photos.map { it.contentUri }
        return mediaStoreManager.createDeleteIntentSender(uris)
    }

    override suspend fun deleteFromDatabaseMultiple(ids: List<Long>) = withContext(Dispatchers.IO) {
        val photos = ids.mapNotNull { photoDao.getPhotoById(it) }
        for (photo in photos) {
            mediaStoreManager.deletePhotoFromMediaStore(photo.contentUri)
            MediaStoreThumbnailFetcher.clearCacheForId(context, photo.id)
        }
        photoDao.deletePhotosByIds(ids)
    }

    // ── Media Operations ───────────────────────────────────────────────

    override suspend fun copyPhotosToAlbum(ids: List<Long>, targetAlbumName: String): Boolean = withContext(Dispatchers.IO) {
        var hasAnySuccess = false
        val resolver = context.contentResolver
        for (id in ids) {
            val photo = photoDao.getPhotoById(id) ?: continue
            val mimeType = photo.mimeType
            val originalName = photo.name
            
            val destDir = getAlbumDirectory(targetAlbumName, mimeType)
            if (!destDir.exists()) destDir.mkdirs()
            
            val destFile = File(destDir, originalName)
            var finalDestFile = destFile
            if (finalDestFile.exists()) {
                val extensionIndex = originalName.lastIndexOf('.')
                val nameWithoutExt = if (extensionIndex != -1) originalName.substring(0, extensionIndex) else originalName
                val ext = if (extensionIndex != -1) originalName.substring(extensionIndex) else ""
                var count = 1
                while (finalDestFile.exists()) {
                    finalDestFile = File(destDir, "${nameWithoutExt}_$count$ext")
                    count++
                }
            }
            
            try {
                resolver.openInputStream(photo.contentUri)?.use { input ->
                    finalDestFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                scanFileWithTimeout(finalDestFile.absolutePath, mimeType)
                hasAnySuccess = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (hasAnySuccess) {
            syncPhotos()
            syncAlbums()
        }
        hasAnySuccess
    }

    override suspend fun movePhotosToAlbum(ids: List<Long>, targetAlbumName: String): Boolean = withContext(Dispatchers.IO) {
        var hasAnySuccess = false
        val resolver = context.contentResolver
        for (id in ids) {
            val photo = photoDao.getPhotoById(id) ?: continue
            val mimeType = photo.mimeType
            val originalName = photo.name
            
            val destDir = getAlbumDirectory(targetAlbumName, mimeType)
            if (!destDir.exists()) destDir.mkdirs()
            
            val destFile = File(destDir, originalName)
            var finalDestFile = destFile
            if (finalDestFile.exists()) {
                val extensionIndex = originalName.lastIndexOf('.')
                val nameWithoutExt = if (extensionIndex != -1) originalName.substring(0, extensionIndex) else originalName
                val ext = if (extensionIndex != -1) originalName.substring(extensionIndex) else ""
                var count = 1
                while (finalDestFile.exists()) {
                    finalDestFile = File(destDir, "${nameWithoutExt}_$count$ext")
                    count++
                }
            }
            
            try {
                resolver.openInputStream(photo.contentUri)?.use { input ->
                    finalDestFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val sourceFile = File(photo.path)
                if (sourceFile.exists()) {
                    sourceFile.delete()
                }
                
                try {
                    resolver.delete(photo.contentUri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                photoDao.deletePhoto(photo)
                
                scanFileWithTimeout(finalDestFile.absolutePath, mimeType)
                hasAnySuccess = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (hasAnySuccess) {
            syncPhotos()
            syncAlbums()
        }
        hasAnySuccess
    }

    override suspend fun renamePhoto(id: Long, newName: String): Boolean = withContext(Dispatchers.IO) {
        val photo = photoDao.getPhotoById(id) ?: return@withContext false
        
        val extensionIndex = photo.name.lastIndexOf('.')
        val ext = if (extensionIndex != -1) photo.name.substring(extensionIndex) else ""
        val finalName = if (newName.endsWith(ext, ignoreCase = true)) newName else "$newName$ext"
        
        val success = renamePhysicalFile(photo, finalName)
        if (success) {
            MediaStoreThumbnailFetcher.clearCacheForId(context, id)
            syncPhotos()
            syncAlbums()
        }
        success
    }

    override suspend fun renamePhotos(ids: List<Long>, baseName: String): Boolean = withContext(Dispatchers.IO) {
        var hasAnySuccess = false
        for ((index, id) in ids.withIndex()) {
            val photo = photoDao.getPhotoById(id) ?: continue
            val extensionIndex = photo.name.lastIndexOf('.')
            val ext = if (extensionIndex != -1) photo.name.substring(extensionIndex) else ""
            val finalName = "${baseName}_${index + 1}$ext"
            if (renamePhysicalFile(photo, finalName)) {
                MediaStoreThumbnailFetcher.clearCacheForId(context, id)
                hasAnySuccess = true
            }
        }
        if (hasAnySuccess) {
            syncPhotos()
            syncAlbums()
        }
        hasAnySuccess
    }

    private suspend fun renamePhysicalFile(photo: PhotoEntity, finalName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(photo.path)
            if (!oldFile.exists()) return@withContext false
            val newFile = File(oldFile.parentFile, finalName)
            if (oldFile.renameTo(newFile)) {
                scanFileWithTimeout(newFile.absolutePath, photo.mimeType)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun scanFileWithTimeout(path: String, mimeType: String): Uri? {
        return kotlinx.coroutines.withTimeoutOrNull(1500) {
            val deferred = kotlinx.coroutines.CompletableDeferred<Uri?>()
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(path),
                arrayOf(mimeType)
            ) { _, uri ->
                deferred.complete(uri)
            }
            deferred.await()
        }
    }

    override suspend fun convertPhotosToPdf(ids: List<Long>, targetFileName: String): Uri? = withContext(Dispatchers.IO) {
        val photos = ids.mapNotNull { photoDao.getPhotoById(it) }
        if (photos.isEmpty()) return@withContext null
        
        val pdfDocument = PdfDocument()
        
        for ((index, photo) in photos.withIndex()) {
            try {
                val bitmap = com.photoapp.util.ImageUtils.loadBitmap(context, photo.contentUri)
                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val finalName = if (targetFileName.endsWith(".pdf", ignoreCase = true)) targetFileName else "$targetFileName.pdf"
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val appDir = File(documentsDir, "PhotoApp")
        if (!appDir.exists()) appDir.mkdirs()
        
        var pdfFile = File(appDir, finalName)
        if (pdfFile.exists()) {
            val extensionIndex = finalName.lastIndexOf('.')
            val nameWithoutExt = if (extensionIndex != -1) finalName.substring(0, extensionIndex) else finalName
            val ext = if (extensionIndex != -1) finalName.substring(extensionIndex) else ""
            var count = 1
            while (pdfFile.exists()) {
                pdfFile = File(appDir, "${nameWithoutExt}_$count$ext")
                count++
            }
        }
        
        var pdfUri: Uri? = null
        try {
            pdfFile.outputStream().use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfUri = scanFileWithTimeout(pdfFile.absolutePath, "application/pdf")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
        
        pdfUri
    }

    override suspend fun setAsWallpaper(id: Long): Boolean = withContext(Dispatchers.IO) {
        val photo = photoDao.getPhotoById(id) ?: return@withContext false
        return@withContext try {
            val bitmap = com.photoapp.util.ImageUtils.loadBitmap(context, photo.contentUri)
            if (bitmap != null) {
                val wallpaperManager = WallpaperManager.getInstance(context)
                wallpaperManager.setBitmap(bitmap)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ── Hidden ──────────────────────────────────────────────────────────

    override fun getHiddenPhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getHiddenPhotos()
    }

    override suspend fun toggleHidden(id: Long) = withContext(Dispatchers.IO) {
        val photo = photoDao.getPhotoById(id) ?: return@withContext
        if (photo.isHidden) {
            unhidePhotos(listOf(id))
        } else {
            hidePhotos(listOf(id))
        }
    }

    override suspend fun hidePhotos(ids: List<Long>): Unit = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val hiddenDir = context.getExternalFilesDir("Hidden") ?: File(context.filesDir, "Hidden")
        if (!hiddenDir.exists()) hiddenDir.mkdirs()

        for (id in ids) {
            val photo = photoDao.getPhotoById(id) ?: continue
            val sourceFile = File(photo.path)
            if (!sourceFile.exists()) continue

            val destFile = File(hiddenDir, photo.name)
            var finalDestFile = destFile
            if (finalDestFile.exists()) {
                val extensionIndex = photo.name.lastIndexOf('.')
                val nameWithoutExt = if (extensionIndex != -1) photo.name.substring(0, extensionIndex) else photo.name
                val ext = if (extensionIndex != -1) photo.name.substring(extensionIndex) else ""
                var count = 1
                while (finalDestFile.exists()) {
                    finalDestFile = File(hiddenDir, "${nameWithoutExt}_$count$ext")
                    count++
                }
            }

            try {
                sourceFile.inputStream().use { input ->
                    finalDestFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                resolver.delete(photo.contentUri, null, null)
                if (sourceFile.exists()) {
                    sourceFile.delete()
                }

                val updatedPhoto = photo.copy(
                    uri = Uri.fromFile(finalDestFile).toString(),
                    path = finalDestFile.absolutePath,
                    isHidden = true
                )
                photoDao.updatePhoto(updatedPhoto)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun unhidePhotos(ids: List<Long>): Unit = withContext(Dispatchers.IO) {
        for (id in ids) {
            val photo = photoDao.getPhotoById(id) ?: continue
            val sourceFile = File(photo.path)
            if (!sourceFile.exists()) continue

            val albumName = photo.bucketName ?: "Restored"
            val destDir = getAlbumDirectory(albumName, photo.mimeType)
            if (!destDir.exists()) destDir.mkdirs()

            val destFile = File(destDir, photo.name)
            var finalDestFile = destFile
            if (finalDestFile.exists()) {
                val extensionIndex = photo.name.lastIndexOf('.')
                val nameWithoutExt = if (extensionIndex != -1) photo.name.substring(0, extensionIndex) else photo.name
                val ext = if (extensionIndex != -1) photo.name.substring(extensionIndex) else ""
                var count = 1
                while (finalDestFile.exists()) {
                    finalDestFile = File(destDir, "${nameWithoutExt}_$count$ext")
                    count++
                }
            }

            try {
                sourceFile.inputStream().use { input ->
                    finalDestFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (sourceFile.exists()) {
                    sourceFile.delete()
                }

                photoDao.deletePhoto(photo)
                scanFileWithTimeout(finalDestFile.absolutePath, photo.mimeType)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        syncPhotos()
        syncAlbums()
    }

    override fun getHiddenCount(): Flow<Int> {
        return photoDao.getHiddenCount()
    }

    private fun getAlbumDirectory(albumName: String, mimeType: String): File {
        return if (albumName.equals("Camera", ignoreCase = true)) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
        } else {
            val subDir = if (mimeType.startsWith("video/")) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            File(Environment.getExternalStoragePublicDirectory(subDir), albumName)
        }
    }
}
