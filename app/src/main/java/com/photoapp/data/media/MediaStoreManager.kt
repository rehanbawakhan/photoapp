package com.photoapp.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.photoapp.data.local.entities.AlbumEntity
import com.photoapp.data.local.entities.PhotoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    private val imageCollection: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    private val videoCollection: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

    private val imageProjection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )

    private val videoProjection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DATE_TAKEN,
        MediaStore.Video.Media.DATE_MODIFIED,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.MIME_TYPE,
        MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME
    )

    // Offset for video IDs in the database to prevent clashes with image IDs
    private val VIDEO_ID_OFFSET = 1_000_000_000_000_000L

    suspend fun loadPhotos(): List<PhotoEntity> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<PhotoEntity>()

        // 1. Load Images
        val imageSortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        contentResolver.query(
            imageCollection,
            imageProjection,
            null,
            null,
            imageSortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )

                val dateTaken = cursor.getLong(dateTakenColumn)
                val dateAdded = cursor.getLong(dateAddedColumn) * 1000 // Convert to millis

                mediaList.add(
                    PhotoEntity(
                        id = id,
                        uri = contentUri.toString(),
                        name = cursor.getString(nameColumn) ?: "Unknown",
                        path = cursor.getString(dataColumn) ?: "",
                        dateAdded = dateAdded,
                        dateTaken = if (dateTaken > 0) dateTaken else dateAdded,
                        dateModified = cursor.getLong(dateModifiedColumn) * 1000,
                        size = cursor.getLong(sizeColumn),
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn),
                        mimeType = cursor.getString(mimeTypeColumn) ?: "image/*",
                        bucketId = cursor.getString(bucketIdColumn),
                        bucketName = cursor.getString(bucketNameColumn)
                    )
                )
            }
        }

        // 2. Load Videos
        val videoSortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        contentResolver.query(
            videoCollection,
            videoProjection,
            null,
            null,
            videoSortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                // Use offset for video primary keys to avoid overlapping with image IDs
                val dbId = id + VIDEO_ID_OFFSET
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )

                val dateTaken = cursor.getLong(dateTakenColumn)
                val dateAdded = cursor.getLong(dateAddedColumn) * 1000 // Convert to millis

                mediaList.add(
                    PhotoEntity(
                        id = dbId,
                        uri = contentUri.toString(),
                        name = cursor.getString(nameColumn) ?: "Unknown",
                        path = cursor.getString(dataColumn) ?: "",
                        dateAdded = dateAdded,
                        dateTaken = if (dateTaken > 0) dateTaken else dateAdded,
                        dateModified = cursor.getLong(dateModifiedColumn) * 1000,
                        size = cursor.getLong(sizeColumn),
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn),
                        mimeType = cursor.getString(mimeTypeColumn) ?: "video/*",
                        bucketId = cursor.getString(bucketIdColumn),
                        bucketName = cursor.getString(bucketNameColumn)
                    )
                )
            }
        }

        // Sort combined list by date taken descending
        mediaList.sortByDescending { it.dateTaken }
        mediaList
    }

    suspend fun loadAlbums(): List<AlbumEntity> = withContext(Dispatchers.IO) {
        val albumMap = mutableMapOf<String, AlbumData>()

        val albumProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        // 1. Process Images
        contentResolver.query(
            imageCollection,
            albumProjection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val photoId = cursor.getLong(idColumn)
                val bucketId = cursor.getString(bucketIdColumn) ?: continue
                val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoId
                )

                albumMap.getOrPut(bucketId) {
                    AlbumData(
                        id = bucketId,
                        name = bucketName,
                        coverUri = contentUri.toString(),
                        count = 0
                    )
                }.count++
            }
        }

        // 2. Process Videos
        val videoAlbumProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        val videoSortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"

        contentResolver.query(
            videoCollection,
            videoAlbumProjection,
            null,
            null,
            videoSortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val videoId = cursor.getLong(idColumn)
                val bucketId = cursor.getString(bucketIdColumn) ?: continue
                val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId
                )

                val existing = albumMap[bucketId]
                if (existing != null) {
                    existing.count++
                } else {
                    albumMap[bucketId] = AlbumData(
                        id = bucketId,
                        name = bucketName,
                        coverUri = contentUri.toString(),
                        count = 1
                    )
                }
            }
        }

        albumMap.values.map { data ->
            AlbumEntity(
                id = data.id,
                name = data.name,
                coverPhotoUri = data.coverUri,
                photoCount = data.count,
                isCustom = false
            )
        }.sortedByDescending { it.photoCount }
    }

    suspend fun deletePhotoFromMediaStore(photoUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val deletedRows = contentResolver.delete(photoUri, null, null)
            deletedRows > 0
        } catch (e: SecurityException) {
            // On Android 11+, this may throw RecoverableSecurityException
            // which needs to be handled by the caller for user confirmation
            false
        } catch (e: Exception) {
            false
        }
    }

    fun createDeleteIntentSender(uris: List<Uri>): android.content.IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                return MediaStore.createDeleteRequest(contentResolver, uris).intentSender
            } catch (e: Exception) {
                return null
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                for (uri in uris) {
                    contentResolver.delete(uri, null, null)
                }
                return null
            } catch (e: SecurityException) {
                val recoverableSecurityException = e as? android.app.RecoverableSecurityException
                    ?: (e.cause as? android.app.RecoverableSecurityException)
                return recoverableSecurityException?.userAction?.actionIntent?.intentSender
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    private data class AlbumData(
        val id: String,
        val name: String,
        val coverUri: String,
        var count: Int
    )
}
