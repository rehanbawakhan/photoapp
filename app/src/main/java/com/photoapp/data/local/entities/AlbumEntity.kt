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
    val isCustom: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis()
)
