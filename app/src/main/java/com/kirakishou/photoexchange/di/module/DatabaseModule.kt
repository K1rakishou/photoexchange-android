package com.kirakishou.photoexchange.di.module

import android.arch.persistence.room.Room
import android.content.Context
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/4/2018.
 */

@Module
open class DatabaseModule(
    val dbName: String
) {

    @Singleton
    @Provides
    open fun provideDatabase(context: Context): MyDatabase {
        return Room.databaseBuilder(context, MyDatabase::class.java, dbName).build()
    }


    @Singleton
    @Provides
    open fun provideMyPhotoRepository(context: Context,
                                      database: MyDatabase): PhotosRepository {
        val filesDir = context.filesDir.absolutePath
        return PhotosRepository(filesDir, database)
    }

    @Singleton
    @Provides
    open fun provideSettingsRepository(database: MyDatabase): SettingsRepository {
        return SettingsRepository(database)
    }

    @Singleton
    @Provides
    open fun providePhotoAnswerRepository(database: MyDatabase): PhotoAnswerRepository {
        return PhotoAnswerRepository(database)
    }

    @Singleton
    @Provides
    open fun provideGalleryPhotoRepository(database: MyDatabase): GalleryPhotoRepository {
        return GalleryPhotoRepository(database)
    }
}