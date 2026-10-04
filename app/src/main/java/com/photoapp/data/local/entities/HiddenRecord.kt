package com.photoapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_records")
data class HiddenRecord(
    @PrimaryKey
    val path: String,
    val originalPath: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val dateTaken: Long,
    val dateModified: Long,
    val bucketId: String? = null,
    val bucketName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
