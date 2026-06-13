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
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

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

    override suspend fun syncPhotos() {
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
                toInsert.add(mediaPhoto)
            } else if (existing.dateModified != mediaPhoto.dateModified || 
                       existing.size != mediaPhoto.size || 
                       existing.path != mediaPhoto.path) {
                // Modified photo: copy favorite/deleted states
                toInsert.add(
                    mediaPhoto.copy(
                        isFavorite = existing.isFavorite,
                        isDeleted = existing.isDeleted,
                        dateDeleted = existing.dateDeleted
                    )
                )
            }
        }

        // Find deleted photos (exist in local DB but no longer in MediaStore)
        val mediaIds = mediaPhotos.map { it.id }.toSet()
        for (existing in existingPhotos) {
            if (existing.id !in mediaIds) {
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
    }

    // ── Favorites ───────────────────────────────────────────────────────

    override fun getFavoritePhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getFavoritePhotos()
    }

    override suspend fun toggleFavorite(id: Long) {
        val photo = photoDao.getPhotoById(id) ?: return
        photoDao.setFavorite(id, !photo.isFavorite)
    }

    override suspend fun setFavoriteMultiple(ids: List<Long>) {
        photoDao.setFavoriteMultiple(ids)
    }

    // ── Trash ───────────────────────────────────────────────────────────

    override fun getTrashPhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getTrashPhotos()
    }

    override suspend fun moveToTrash(id: Long) {
        photoDao.moveToTrash(id)
    }

    override suspend fun moveToTrashMultiple(ids: List<Long>) {
        photoDao.moveToTrashMultiple(ids)
    }

    override suspend fun restoreFromTrash(id: Long) {
        photoDao.restoreFromTrash(id)
    }

    override suspend fun restoreAllFromTrash() {
        val trashPhotos = photoDao.getExpiredTrashPhotos(Long.MAX_VALUE)
        val ids = trashPhotos.map { it.id }
        photoDao.restoreAllFromTrash(ids)
    }

    override suspend fun permanentlyDelete(id: Long) {
        val photo = photoDao.getPhotoById(id) ?: return
        // Delete from device storage
        mediaStoreManager.deletePhotoFromMediaStore(photo.contentUri)
        // Remove from database
        photoDao.deletePhoto(photo)
    }

    override suspend fun emptyTrash() {
        val trashPhotos = photoDao.getExpiredTrashPhotos(Long.MAX_VALUE)
        for (photo in trashPhotos) {
            mediaStoreManager.deletePhotoFromMediaStore(photo.contentUri)
        }
        photoDao.emptyTrash()
    }

    override suspend fun cleanupExpiredTrash() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val expiredPhotos = photoDao.getExpiredTrashPhotos(thirtyDaysAgo)
        for (photo in expiredPhotos) {
            mediaStoreManager.deletePhotoFromMediaStore(photo.contentUri)
            photoDao.deletePhoto(photo)
        }
    }

    // ── Albums ──────────────────────────────────────────────────────────

    override fun getAllAlbums(): Flow<List<AlbumEntity>> {
        return photoDao.getAllAlbums()
    }

    override fun getPhotosByBucket(bucketId: String): Flow<List<PhotoEntity>> {
        return photoDao.getPhotosByBucket(bucketId)
    }

    override suspend fun syncAlbums() {
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
        }
        return deleted
    }

    override suspend fun getDeleteIntentSender(ids: List<Long>): android.content.IntentSender? {
        val photos = ids.mapNotNull { photoDao.getPhotoById(it) }
        if (photos.isEmpty()) return null
        val uris = photos.map { it.contentUri }
        return mediaStoreManager.createDeleteIntentSender(uris)
    }

    override suspend fun deleteFromDatabaseMultiple(ids: List<Long>) {
        val photos = ids.mapNotNull { photoDao.getPhotoById(it) }
        for (photo in photos) {
            mediaStoreManager.deletePhotoFromMediaStore(photo.contentUri)
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
            
            val subDir = if (mimeType.startsWith("video/")) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val targetRelativePath = "$subDir/$targetAlbumName"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, originalName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                
                val collection = if (mimeType.startsWith("video/")) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }
                
                val newUri = resolver.insert(collection, contentValues)
                if (newUri != null) {
                    try {
                        resolver.openInputStream(photo.contentUri)?.use { input ->
                            resolver.openOutputStream(newUri)?.use { output ->
                                input.copyTo(output)
                            }
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(newUri, contentValues, null, null)
                        hasAnySuccess = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        resolver.delete(newUri, null, null)
                    }
                }
            } else {
                val destDir = File(Environment.getExternalStoragePublicDirectory(subDir), targetAlbumName)
                if (!destDir.exists()) destDir.mkdirs()
                val destFile = File(destDir, originalName)
                try {
                    resolver.openInputStream(photo.contentUri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    android.media.MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
                    hasAnySuccess = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
            
            val subDir = if (mimeType.startsWith("video/")) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            
            var moveSuccess = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val targetRelativePath = "$subDir/$targetAlbumName"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, originalName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                
                val collection = if (mimeType.startsWith("video/")) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }
                
                val newUri = resolver.insert(collection, contentValues)
                if (newUri != null) {
                    try {
                        resolver.openInputStream(photo.contentUri)?.use { input ->
                            resolver.openOutputStream(newUri)?.use { output ->
                                input.copyTo(output)
                            }
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(newUri, contentValues, null, null)
                        
                        // Delete the original media
                        resolver.delete(photo.contentUri, null, null)
                        photoDao.deletePhoto(photo)
                        moveSuccess = true
                        hasAnySuccess = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        resolver.delete(newUri, null, null)
                    }
                }
            } else {
                val destDir = File(Environment.getExternalStoragePublicDirectory(subDir), targetAlbumName)
                if (!destDir.exists()) destDir.mkdirs()
                val destFile = File(destDir, originalName)
                try {
                    resolver.openInputStream(photo.contentUri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    android.media.MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
                    
                    // Delete original
                    resolver.delete(photo.contentUri, null, null)
                    photoDao.deletePhoto(photo)
                    moveSuccess = true
                    hasAnySuccess = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
        val resolver = context.contentResolver
        
        val extensionIndex = photo.name.lastIndexOf('.')
        val ext = if (extensionIndex != -1) photo.name.substring(extensionIndex) else ""
        val finalName = if (newName.endsWith(ext, ignoreCase = true)) newName else "$newName$ext"
        
        return@withContext try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
            }
            val rows = resolver.update(photo.contentUri, contentValues, null, null)
            if (rows > 0) {
                photoDao.insertPhoto(photo.copy(name = finalName))
                syncPhotos()
                syncAlbums()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun renamePhotos(ids: List<Long>, baseName: String): Boolean = withContext(Dispatchers.IO) {
        var hasAnySuccess = false
        val resolver = context.contentResolver
        ids.forEachIndexed { index, id ->
            val photo = photoDao.getPhotoById(id) ?: return@forEachIndexed
            val extensionIndex = photo.name.lastIndexOf('.')
            val ext = if (extensionIndex != -1) photo.name.substring(extensionIndex) else ""
            val finalName = "${baseName}_${index + 1}$ext"
            
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                }
                val rows = resolver.update(photo.contentUri, contentValues, null, null)
                if (rows > 0) {
                    photoDao.insertPhoto(photo.copy(name = finalName))
                    hasAnySuccess = true
                }
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

    override suspend fun convertPhotosToPdf(ids: List<Long>, targetFileName: String): Uri? = withContext(Dispatchers.IO) {
        val photos = ids.mapNotNull { photoDao.getPhotoById(it) }
        if (photos.isEmpty()) return@withContext null
        
        val pdfDocument = PdfDocument()
        val resolver = context.contentResolver
        
        for ((index, photo) in photos.withIndex()) {
            try {
                resolver.openInputStream(photo.contentUri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(page)
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val finalName = if (targetFileName.endsWith(".pdf", ignoreCase = true)) targetFileName else "$targetFileName.pdf"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/PhotoApp")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        
        var pdfUri: Uri? = null
        try {
            pdfUri = resolver.insert(collection, contentValues)
            if (pdfUri != null) {
                resolver.openOutputStream(pdfUri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(pdfUri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (pdfUri != null) {
                resolver.delete(pdfUri, null, null)
                pdfUri = null
            }
        } finally {
            pdfDocument.close()
        }
        
        pdfUri
    }

    override suspend fun setAsWallpaper(id: Long): Boolean = withContext(Dispatchers.IO) {
        val photo = photoDao.getPhotoById(id) ?: return@withContext false
        val resolver = context.contentResolver
        return@withContext try {
            resolver.openInputStream(photo.contentUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    wallpaperManager.setBitmap(bitmap)
                    true
                } else {
                    false
                }
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
