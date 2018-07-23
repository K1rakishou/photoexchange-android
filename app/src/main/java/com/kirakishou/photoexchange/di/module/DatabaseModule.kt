package com.kirakishou.photoexchange.di.module

import android.arch.persistence.room.Room
import android.content.Context
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.other.Constants.RECEIVED_PHOTOS_CACHE_MAX_LIVE_TIME
import com.kirakishou.photoexchange.mvp.model.other.Constants.UPLOADED_PHOTOS_CACHE_MAX_LIVE_TIME
import dagger.Module
import dagger.Provides
import javax.inject.Named
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
    @Named("files_directory")
    fun provideFilesDirectoryPath(context: Context): String {
        return context.filesDir.absolutePath
    }

    @Singleton
    @Provides
    fun provideTempFilesRepository(@Named("files_directory") filesDir: String,
                                   database: MyDatabase,
                                   timeUtils: TimeUtils,
                                   fileUtils: FileUtils): TempFileRepository {
        return TempFileRepository(filesDir, database, timeUtils, fileUtils)
    }

    @Singleton
    @Provides
    open fun provideTakenPhotoRepository(database: MyDatabase,
                                         timeUtils: TimeUtils,
                                         tempFileRepository: TempFileRepository): TakenPhotosRepository {
        return TakenPhotosRepository(timeUtils, database, tempFileRepository)
    }

    @Singleton
    @Provides
    open fun provideSettingsRepository(database: MyDatabase): SettingsRepository {
        return SettingsRepository(database)
    }

    @Singleton
    @Provides
    open fun provideReceivedPhotoRepository(database: MyDatabase, timeUtils: TimeUtils): ReceivedPhotosRepository {
        return ReceivedPhotosRepository(database, timeUtils, RECEIVED_PHOTOS_CACHE_MAX_LIVE_TIME)
    }

    @Singleton
    @Provides
    open fun provideGalleryPhotoRepository(database: MyDatabase, timeUtils: TimeUtils): GalleryPhotoRepository {
        return GalleryPhotoRepository(database, timeUtils)
    }

    @Singleton
    @Provides
    open fun provideUploadedPhotoRepository(database: MyDatabase, timeUtils: TimeUtils): UploadedPhotosRepository {
        return UploadedPhotosRepository(database, timeUtils, UPLOADED_PHOTOS_CACHE_MAX_LIVE_TIME)
    }

    @Singleton
    @Provides
    open fun provideCachedPhotoIdRepository(database: MyDatabase): CachedPhotoIdRepository {
        return CachedPhotoIdRepository(database)
    }
}