package com.photoapp.data.repository

import android.net.Uri
import com.photoapp.data.local.entities.AlbumEntity
import com.photoapp.data.local.entities.PhotoEntity
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    // Photos
    fun getAllPhotos(): Flow<List<PhotoEntity>>
    suspend fun getPhotoById(id: Long): PhotoEntity?
    fun observePhotoById(id: Long): Flow<PhotoEntity?>
    suspend fun syncPhotos()

    // Favorites
    fun getFavoritePhotos(): Flow<List<PhotoEntity>>
    suspend fun toggleFavorite(id: Long)
    suspend fun setFavoriteMultiple(ids: List<Long>)

    // Trash
    fun getTrashPhotos(): Flow<List<PhotoEntity>>
    suspend fun moveToTrash(id: Long)
    suspend fun moveToTrashMultiple(ids: List<Long>)
    suspend fun restoreFromTrash(id: Long)
    suspend fun restoreAllFromTrash()
    suspend fun permanentlyDelete(id: Long)
    suspend fun emptyTrash()
    suspend fun cleanupExpiredTrash()

    // Albums
    fun getAllAlbums(): Flow<List<AlbumEntity>>
    fun getPhotosByBucket(bucketId: String): Flow<List<PhotoEntity>>
    suspend fun syncAlbums()

    // Search
    fun searchPhotos(query: String): Flow<List<PhotoEntity>>

    // Share
    suspend fun getShareUri(photoId: Long): Uri?

    // Delete from device
    suspend fun deleteFromDevice(photoId: Long): Boolean
    suspend fun getDeleteIntentSender(ids: List<Long>): android.content.IntentSender?
    suspend fun deleteFromDatabaseMultiple(ids: List<Long>)

    // Media Operations
    suspend fun copyPhotosToAlbum(ids: List<Long>, targetAlbumName: String): Boolean
    suspend fun movePhotosToAlbum(ids: List<Long>, targetAlbumName: String): Boolean
    suspend fun renamePhoto(id: Long, newName: String): Boolean
    suspend fun renamePhotos(ids: List<Long>, baseName: String): Boolean
    suspend fun convertPhotosToPdf(ids: List<Long>, targetFileName: String): Uri?
    suspend fun setAsWallpaper(id: Long): Boolean
}
