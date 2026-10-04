package com.photoapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_records")
data class FavoriteRecord(
    @PrimaryKey
    val path: String,
    val name: String,
    val size: Long,
    val dateAdded: Long = System.currentTimeMillis()
)
