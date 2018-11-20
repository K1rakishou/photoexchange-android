package com.kirakishou.photoexchange.di.module

import androidx.room.Room
import android.content.Context
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.database.source.local.*
import com.kirakishou.photoexchange.helper.database.source.remote.*
import com.kirakishou.photoexchange.helper.util.BitmapUtils
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

  @Singleton
  @Provides
  open fun provideReceivePhotosLocalSource(database: MyDatabase,
                                           timeUtils: TimeUtils): ReceivePhotosLocalSource {
    return ReceivePhotosLocalSource(database, timeUtils, RECEIVED_PHOTOS_CACHE_MAX_LIVE_TIME)
  }

  @Singleton
  @Provides
  open fun provideUploadPhotosLocalSource(database: MyDatabase,
                                          timeUtils: TimeUtils): UploadPhotosLocalSource {
    return UploadPhotosLocalSource(database, timeUtils, UPLOADED_PHOTOS_CACHE_MAX_LIVE_TIME)
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

  @Singleton
  @Provides
  open fun provideFavouritePhotoRemoteSource(apiClient: ApiClient): FavouritePhotoRemoteSource {
    return FavouritePhotoRemoteSource(apiClient)
  }

  @Singleton
  @Provides
  open fun provideReportPhotoRemoteSource(apiClient: ApiClient): ReportPhotoRemoteSource {
    return ReportPhotoRemoteSource(apiClient)
  }

  @Singleton
  @Provides
  open fun provideGetReceivedPhotosRemoteSource(apiClient: ApiClient): GetReceivedPhotosRemoteSource {
    return GetReceivedPhotosRemoteSource(apiClient)
  }

  @Singleton
  @Provides
  open fun provideUploadPhotosRemoteSource(apiClient: ApiClient): UploadPhotosRemoteSource {
    return UploadPhotosRemoteSource(apiClient)
  }

  @Singleton
  @Provides
  open fun provideReceivePhotosRemoteSource(apiClient: ApiClient): ReceivePhotosRemoteSource {
    return ReceivePhotosRemoteSource(apiClient)
  }

  @Singleton
  @Provides
  open fun provideGetUploadedPhotosRemoteSource(apiClient: ApiClient): GetUploadedPhotosRemoteSource {
    return GetUploadedPhotosRemoteSource(apiClient)
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
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideGalleryPhotoRepository(database: MyDatabase,
                                         settingsLocalSource: SettingsLocalSource,
                                         galleryPhotoRemoteSource: GalleryPhotoRemoteSource,
                                         galleryPhotoLocalSource: GalleryPhotoLocalSource,
                                         galleryPhotoInfoRemoteSource: GalleryPhotoInfoRemoteSource,
                                         galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
                                         dispatchersProvider: DispatchersProvider): GetGalleryPhotosRepository {
    return GetGalleryPhotosRepository(
      database,
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

  @Singleton
  @Provides
  open fun provideFavouritePhotoRepository(database: MyDatabase,
                                           timeUtils: TimeUtils,
                                           favouritePhotoRemoteSource: FavouritePhotoRemoteSource,
                                           galleryPhotoLocalSource: GalleryPhotoLocalSource,
                                           galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
                                           dispatchersProvider: DispatchersProvider): FavouritePhotoRepository {
    return FavouritePhotoRepository(
      database,
      timeUtils,
      favouritePhotoRemoteSource,
      galleryPhotoLocalSource,
      galleryPhotoInfoLocalSource,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideReportPhotoRepository(database: MyDatabase,
                                        timeUtils: TimeUtils,
                                        reportPhotoRemoteSource: ReportPhotoRemoteSource,
                                        galleryPhotoLocalSource: GalleryPhotoLocalSource,
                                        galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
                                        dispatchersProvider: DispatchersProvider): ReportPhotoRepository {
    return ReportPhotoRepository(
      database,
      timeUtils,
      reportPhotoRemoteSource,
      galleryPhotoLocalSource,
      galleryPhotoInfoLocalSource,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideGetReceivedPhotosRepository(database: MyDatabase,
                                              getReceivedPhotosRemoteSource: GetReceivedPhotosRemoteSource,
                                              receivePhotosLocalSource: ReceivePhotosLocalSource,
                                              uploadedPhotosLocalSource: UploadPhotosLocalSource): GetReceivedPhotosRepository {
    return GetReceivedPhotosRepository(
      database,
      getReceivedPhotosRemoteSource,
      receivePhotosLocalSource,
      uploadedPhotosLocalSource
    )
  }

  @Singleton
  @Provides
  open fun provideUploadPhotosRepository(database: MyDatabase,
                                         timeUtils: TimeUtils,
                                         bitmapUtils: BitmapUtils,
                                         fileUtils: FileUtils,
                                         takenPhotosLocalSource: TakenPhotosLocalSource,
                                         uploadPhotosRemoteSource: UploadPhotosRemoteSource,
                                         uploadPhotosLocalSource: UploadPhotosLocalSource): UploadPhotosRepository {
    return UploadPhotosRepository(
      database,
      timeUtils,
      bitmapUtils,
      fileUtils,
      takenPhotosLocalSource,
      uploadPhotosRemoteSource,
      uploadPhotosLocalSource
    )
  }

  @Singleton
  @Provides
  open fun provideTakenPhotosLocalSource(database: MyDatabase,
                                         timeUtils: TimeUtils): TakenPhotosLocalSource {
    return TakenPhotosLocalSource(database, timeUtils)
  }

  @Singleton
  @Provides
  open fun provideReceivePhotosRepository(database: MyDatabase,
                                          receivePhotosRemoteSource: ReceivePhotosRemoteSource,
                                          receivePhotosLocalSource: ReceivePhotosLocalSource,
                                          uploadedPhotosLocalSource: UploadPhotosLocalSource,
                                          takenPhotosLocalSource: TakenPhotosLocalSource): ReceivePhotosRepository {
    return ReceivePhotosRepository(
      database,
      receivePhotosRemoteSource,
      receivePhotosLocalSource,
      uploadedPhotosLocalSource,
      takenPhotosLocalSource
    )
  }

  @Singleton
  @Provides
  open fun provideGetUploadedPhotosRepository(uploadedPhotosLocalSource: UploadPhotosLocalSource,
                                              getUploadedPhotosRemoteSource: GetUploadedPhotosRemoteSource): GetUploadedPhotosRepository {
    return GetUploadedPhotosRepository(
      uploadedPhotosLocalSource,
      getUploadedPhotosRemoteSource
    )
  }
}