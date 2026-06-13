package com.photoapp.data.local

import androidx.room.*
import com.photoapp.data.local.entities.AlbumEntity
import com.photoapp.data.local.entities.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    // ── Photos ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM photos WHERE isDeleted = 0 AND isHidden = 0 ORDER BY dateTaken DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos")
    suspend fun getAllPhotosList(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): PhotoEntity?

    @Query("SELECT * FROM photos WHERE id = :id")
    fun observePhotoById(id: Long): Flow<PhotoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deletePhotosByIds(ids: List<Long>)

    @Query("DELETE FROM photos")
    suspend fun deleteAllPhotos()

    // ── Favorites ───────────────────────────────────────────────────────

    @Query("SELECT * FROM photos WHERE isFavorite = 1 AND isDeleted = 0 AND isHidden = 0 ORDER BY dateTaken DESC")
    fun getFavoritePhotos(): Flow<List<PhotoEntity>>

    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun setFavoriteMultiple(ids: List<Long>, isFavorite: Boolean)

    // ── Trash ───────────────────────────────────────────────────────────

    @Query("SELECT * FROM photos WHERE isDeleted = 1 ORDER BY dateDeleted DESC")
    fun getTrashPhotos(): Flow<List<PhotoEntity>>

    @Query("UPDATE photos SET isDeleted = 1, dateDeleted = :dateDeleted WHERE id = :id")
    suspend fun moveToTrash(id: Long, dateDeleted: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET isDeleted = 1, dateDeleted = :dateDeleted WHERE id IN (:ids)")
    suspend fun moveToTrashMultiple(ids: List<Long>, dateDeleted: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET isDeleted = 0, dateDeleted = null WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("UPDATE photos SET isDeleted = 0, dateDeleted = null WHERE id IN (:ids)")
    suspend fun restoreAllFromTrash(ids: List<Long>)

    @Query("SELECT * FROM photos WHERE isDeleted = 1 AND dateDeleted < :cutoffTime")
    suspend fun getExpiredTrashPhotos(cutoffTime: Long): List<PhotoEntity>

    @Query("DELETE FROM photos WHERE isDeleted = 1")
    suspend fun emptyTrash()

    // ── Albums ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumById(id: String): AlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Delete
    suspend fun deleteAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE isCustom = 0")
    suspend fun deleteAutoAlbums()

    // ── Album Photos ────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM photos 
        WHERE bucketId = :bucketId AND isDeleted = 0 AND isHidden = 0 
        ORDER BY dateTaken DESC
    """)
    fun getPhotosByBucket(bucketId: String): Flow<List<PhotoEntity>>

    @Query("""
        SELECT * FROM photos 
        WHERE bucketId = :bucketId AND isDeleted = 0 AND isHidden = 0 
        ORDER BY dateTaken DESC
    """)
    suspend fun getPhotosByBucketList(bucketId: String): List<PhotoEntity>

    // ── Search ──────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM photos 
        WHERE isDeleted = 0 AND isHidden = 0 AND (name LIKE '%' || :query || '%' OR path LIKE '%' || :query || '%')
        ORDER BY dateTaken DESC
    """)
    fun searchPhotos(query: String): Flow<List<PhotoEntity>>

    // ── Stats ───────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM photos WHERE isDeleted = 0 AND isHidden = 0")
    fun getPhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE isDeleted = 1")
    fun getTrashCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE isFavorite = 1 AND isDeleted = 0 AND isHidden = 0")
    fun getFavoriteCount(): Flow<Int>

    // ── Hidden ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM photos WHERE isHidden = 1 AND isDeleted = 0 ORDER BY dateTaken DESC")
    fun getHiddenPhotos(): Flow<List<PhotoEntity>>

    @Query("UPDATE photos SET isHidden = :isHidden WHERE id = :id")
    suspend fun setHidden(id: Long, isHidden: Boolean)

    @Query("UPDATE photos SET isHidden = 1 WHERE id IN (:ids)")
    suspend fun hideMultiple(ids: List<Long>)

    @Query("UPDATE photos SET isHidden = 0 WHERE id IN (:ids)")
    suspend fun unhideMultiple(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM photos WHERE isHidden = 1 AND isDeleted = 0")
    fun getHiddenCount(): Flow<Int>
}
