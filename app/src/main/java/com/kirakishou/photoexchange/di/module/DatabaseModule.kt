package com.kirakishou.photoexchange.di.module

import androidx.room.Room
import android.content.Context
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoInfoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.SettingsLocalSource
import com.kirakishou.photoexchange.helper.database.source.remote.GalleryPhotoInfoRemoteSource
import com.kirakishou.photoexchange.helper.database.source.remote.GalleryPhotoRemoteSource
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.other.Constants.GALLERY_PHOTOS_CACHE_MAX_LIVE_TIME
import com.kirakishou.photoexchange.mvp.model.other.Constants.GALLERY_PHOTOS_INFO_CACHE_MAX_LIVE_TIME
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
    return Room.databaseBuilder(context, MyDatabase::class.java, dbName)
      .build()
  }

  @Singleton
  @Provides
  @Named("files_directory")
  fun provideFilesDirectoryPath(context: Context): String {
    return context.filesDir.absolutePath
  }

  /**
   * Local Sources
   * */

  @Singleton
  @Provides
  open fun provideGalleryPhotoLocalSource(database: MyDatabase,
                                          timeUtils: TimeUtils): GalleryPhotoLocalSource {
    return GalleryPhotoLocalSource(database, timeUtils, GALLERY_PHOTOS_CACHE_MAX_LIVE_TIME)
  }

  @Singleton
  @Provides
  open fun provideGalleryPhotoInfoLocalSource(database: MyDatabase,
                                              timeUtils: TimeUtils): GalleryPhotoInfoLocalSource {
    return GalleryPhotoInfoLocalSource(database, timeUtils, GALLERY_PHOTOS_INFO_CACHE_MAX_LIVE_TIME)
  }

  @Singleton
  @Provides
  open fun provideSettingsLocalSource(database: MyDatabase): SettingsLocalSource {
    return SettingsLocalSource(database)
  }

  /**
   * Remote Sources
   * */

  @Singleton
  @Provides
  open fun provideGalleryPhotoRemoteSource(apiClient: ApiClient): GalleryPhotoRemoteSource {
    return GalleryPhotoRemoteSource(apiClient)
  }

  @Singleton
  @Provides
  open fun provideGalleryPhotoInfoRemoteSource(apiClient: ApiClient): GalleryPhotoInfoRemoteSource {
    return GalleryPhotoInfoRemoteSource(apiClient)
  }

  /**
   * Repositories
   * */

  @Singleton
  @Provides
  fun provideTempFilesRepository(@Named("files_directory") filesDir: String,
                                 database: MyDatabase,
                                 timeUtils: TimeUtils,
                                 fileUtils: FileUtils,
                                 dispatchersProvider: DispatchersProvider): TempFileRepository {
    return TempFileRepository(
      filesDir,
      database,
      timeUtils,
      fileUtils,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideTakenPhotoRepository(database: MyDatabase,
                                       timeUtils: TimeUtils,
                                       tempFileRepository: TempFileRepository,
                                       dispatchersProvider: DispatchersProvider): TakenPhotosRepository {
    return TakenPhotosRepository(
      timeUtils,
      database,
      tempFileRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideSettingsRepository(database: MyDatabase,
                                     dispatchersProvider: DispatchersProvider): SettingsRepository {
    return SettingsRepository(database, dispatchersProvider)
  }

  @Singleton
  @Provides
  open fun provideReceivedPhotoRepository(database: MyDatabase,
                                          timeUtils: TimeUtils,
                                          dispatchersProvider: DispatchersProvider): ReceivedPhotosRepository {
    return ReceivedPhotosRepository(
      database,
      timeUtils,
      RECEIVED_PHOTOS_CACHE_MAX_LIVE_TIME,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideGalleryPhotoRepository(database: MyDatabase,
                                         timeUtils: TimeUtils,
                                         settingsLocalSource: SettingsLocalSource,
                                         galleryPhotoRemoteSource: GalleryPhotoRemoteSource,
                                         galleryPhotoLocalSource: GalleryPhotoLocalSource,
                                         galleryPhotoInfoRemoteSource: GalleryPhotoInfoRemoteSource,
                                         galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
                                         dispatchersProvider: DispatchersProvider): GalleryPhotoRepository {
    return GalleryPhotoRepository(
      database,
      timeUtils,
      settingsLocalSource,
      galleryPhotoRemoteSource,
      galleryPhotoLocalSource,
      galleryPhotoInfoRemoteSource,
      galleryPhotoInfoLocalSource,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideUploadedPhotoRepository(database: MyDatabase,
                                          timeUtils: TimeUtils,
                                          dispatchersProvider: DispatchersProvider): UploadedPhotosRepository {
    return UploadedPhotosRepository(
      database,
      timeUtils,
      UPLOADED_PHOTOS_CACHE_MAX_LIVE_TIME,
      dispatchersProvider
    )
  }
}