package com.photoapp.data.local.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey
    val id: Long,
    val uri: String,
    val name: String,
    val path: String,
    val dateAdded: Long,
    val dateTaken: Long,
    val dateModified: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val bucketId: String? = null,
    val bucketName: String? = null,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val dateDeleted: Long? = null,
    val isHidden: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    @Ignore
    @Transient
    private var _contentUri: Uri? = null

    val contentUri: Uri
        get() {
            var c = _contentUri
            if (c == null) {
                c = Uri.parse(uri)
                _contentUri = c
            }
            return c
        }

    val formattedSize: String
        get() {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$size B"
            }
        }

    val resolution: String
        get() = "${width} × ${height}"
}
