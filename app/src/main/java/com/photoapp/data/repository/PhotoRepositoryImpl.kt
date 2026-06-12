package com.photoapp.data.repository

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
    private val mediaStoreManager: MediaStoreManager
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
}
