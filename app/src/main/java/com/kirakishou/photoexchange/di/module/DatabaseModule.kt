package com.kirakishou.photoexchange.di.module

import android.arch.persistence.room.Room
import android.content.Context
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.util.TimeUtils
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
    open fun provideTakenPhotoRepository(context: Context,
                                         database: MyDatabase,
                                         timeUtils: TimeUtils): TakenPhotosRepository {
        val filesDir = context.filesDir.absolutePath
        return TakenPhotosRepository(filesDir, database, timeUtils)
    }

    @Singleton
    @Provides
    open fun provideSettingsRepository(database: MyDatabase): SettingsRepository {
        return SettingsRepository(database)
    }

    @Singleton
    @Provides
    open fun provideReceivedPhotoRepository(database: MyDatabase): ReceivedPhotosRepository {
        return ReceivedPhotosRepository(database)
    }

    @Singleton
    @Provides
    open fun provideGalleryPhotoRepository(database: MyDatabase, timeUtils: TimeUtils): GalleryPhotoRepository {
        return GalleryPhotoRepository(database, timeUtils)
    }

    @Singleton
    @Provides
    open fun provideUploadedPhotoRepository(database: MyDatabase): UploadedPhotosRepository {
        return UploadedPhotosRepository(database)
    }
}