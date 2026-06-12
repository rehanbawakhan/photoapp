package com.photoapp.di

import android.content.Context
import androidx.room.Room
import com.photoapp.data.local.PhotoDao
import com.photoapp.data.local.PhotoDatabase
import com.photoapp.data.repository.PhotoRepository
import com.photoapp.data.repository.PhotoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PhotoDatabase {
        return Room.databaseBuilder(
            context,
            PhotoDatabase::class.java,
            PhotoDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePhotoDao(database: PhotoDatabase): PhotoDao {
        return database.photoDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPhotoRepository(
        impl: PhotoRepositoryImpl
    ): PhotoRepository
}
