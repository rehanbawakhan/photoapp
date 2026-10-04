package com.photoapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val coverPhotoUri: String? = null,
    val photoCount: Int = 0,
    val videoCount: Int = 0,
    val isCustom: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis()
) {
    val formattedCount: String
        get() = when {
            photoCount > 0 && videoCount > 0 -> "$photoCount photos $videoCount videos"
            videoCount > 0 -> "$videoCount videos"
            else -> "$photoCount photos"
        }
}
