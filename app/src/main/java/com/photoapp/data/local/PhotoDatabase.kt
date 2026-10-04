package com.photoapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.photoapp.data.local.entities.AlbumEntity
import com.photoapp.data.local.entities.FavoriteRecord
import com.photoapp.data.local.entities.HiddenRecord
import com.photoapp.data.local.entities.PhotoEntity

@Database(
    entities = [PhotoEntity::class, AlbumEntity::class, FavoriteRecord::class, HiddenRecord::class],
    version = 3,
    exportSchema = false
)
abstract class PhotoDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        const val DATABASE_NAME = "photo_app_database"
    }
}
