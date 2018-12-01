package com.kirakishou.photoexchange.di.module

import androidx.room.Room
import android.content.Context
import com.google.firebase.iid.FirebaseInstanceId
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.database.source.local.*
import com.kirakishou.photoexchange.helper.database.source.remote.*
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.helper.Constants.GALLERY_PHOTOS_CACHE_MAX_LIVE_TIME
import com.kirakishou.photoexchange.helper.Constants.GALLERY_PHOTOS_INFO_CACHE_MAX_LIVE_TIME
import com.kirakishou.photoexchange.helper.Constants.RECEIVED_PHOTOS_CACHE_MAX_LIVE_TIME
import com.kirakishou.photoexchange.helper.Constants.UPLOADED_PHOTOS_CACHE_MAX_LIVE_TIME
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
  open fun provideDatabase(context: Context,
                           dispatchersProvider: DispatchersProvider): MyDatabase {
    return Room.databaseBuilder(context, MyDatabase::class.java, dbName)
      .build()
      .also { it.dispatchersProvider = dispatchersProvider }
  }

  @Singleton
  @Provides
  fun provideFirebaseInstanceId(): FirebaseInstanceId {
    return FirebaseInstanceId.getInstance()
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
    return GalleryPhotoLocalSource(database, timeUtils)
  }

  @Singleton
  @Provides
  open fun provideGalleryPhotoInfoLocalSource(database: MyDatabase,
                                              timeUtils: TimeUtils): GalleryPhotoInfoLocalSource {
    return GalleryPhotoInfoLocalSource(database, timeUtils)
  }

  @Singleton
  @Provides
  open fun provideReceivePhotosLocalSource(database: MyDatabase,
                                           timeUtils: TimeUtils): ReceivePhotosLocalSource {
    return ReceivePhotosLocalSource(database, timeUtils)
  }

  @Singleton
  @Provides
  open fun provideUploadPhotosLocalSource(database: MyDatabase,
                                          timeUtils: TimeUtils): UploadPhotosLocalSource {
    return UploadPhotosLocalSource(database, timeUtils)
  }

  @Singleton
  @Provides
  open fun provideTempFileLocalSource(database: MyDatabase,
                                      @Named("files_directory") filesDir: String,
                                      timeUtils: TimeUtils,
                                      fileUtils: FileUtils): TempFileLocalSource {
    return TempFileLocalSource(database, filesDir, timeUtils, fileUtils)
  }

  /**
   * Remote Sources
   * */

  @Singleton
  @Provides
  fun provideFirebaseRemoteSource(firebaseInstanceId: FirebaseInstanceId): FirebaseRemoteSource {
    return FirebaseRemoteSource(firebaseInstanceId)
  }

  /**
   * Repositories
   * */

  @Singleton
  @Provides
  open fun provideTakenPhotoRepository(database: MyDatabase,
                                       timeUtils: TimeUtils,
                                       takenPhotosLocalSource: TakenPhotosLocalSource,
                                       tempFileLocalSource: TempFileLocalSource,
                                       dispatchersProvider: DispatchersProvider): TakenPhotosRepository {
    return TakenPhotosRepository(
      timeUtils,
      database,
      takenPhotosLocalSource,
      tempFileLocalSource,
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
                                         apiClient: ApiClient,
                                         galleryPhotoLocalSource: GalleryPhotoLocalSource,
                                         galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
                                         dispatchersProvider: DispatchersProvider): GetGalleryPhotosRepository {
    return GetGalleryPhotosRepository(
      database,
      apiClient,
      galleryPhotoLocalSource,
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
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideFavouritePhotoRepository(database: MyDatabase,
                                           timeUtils: TimeUtils,
                                           apiClient: ApiClient,
                                           galleryPhotoLocalSource: GalleryPhotoLocalSource,
                                           galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
                                           dispatchersProvider: DispatchersProvider): FavouritePhotoRepository {
    return FavouritePhotoRepository(
      database,
      timeUtils,
      apiClient,
      galleryPhotoLocalSource,
      galleryPhotoInfoLocalSource,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideReportPhotoRepository(database: MyDatabase,
                                        timeUtils: TimeUtils,
                                        apiClient: ApiClient,
                                        galleryPhotoLocalSource: GalleryPhotoLocalSource,
                                        galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
                                        dispatchersProvider: DispatchersProvider): ReportPhotoRepository {
    return ReportPhotoRepository(
      database,
      timeUtils,
      apiClient,
      galleryPhotoLocalSource,
      galleryPhotoInfoLocalSource,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideGetReceivedPhotosRepository(database: MyDatabase,
                                              apiClient: ApiClient,
                                              receivePhotosLocalSource: ReceivePhotosLocalSource,
                                              uploadedPhotosLocalSource: UploadPhotosLocalSource,
                                              dispatchersProvider: DispatchersProvider): GetReceivedPhotosRepository {
    return GetReceivedPhotosRepository(
      database,
      apiClient,
      receivePhotosLocalSource,
      uploadedPhotosLocalSource,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideUploadPhotosRepository(database: MyDatabase,
                                         timeUtils: TimeUtils,
                                         bitmapUtils: BitmapUtils,
                                         fileUtils: FileUtils,
                                         takenPhotosLocalSource: TakenPhotosLocalSource,
                                         apiClient: ApiClient,
                                         uploadPhotosLocalSource: UploadPhotosLocalSource,
                                         dispatchersProvider: DispatchersProvider): UploadPhotosRepository {
    return UploadPhotosRepository(
      database,
      timeUtils,
      bitmapUtils,
      fileUtils,
      takenPhotosLocalSource,
      apiClient,
      uploadPhotosLocalSource,
      dispatchersProvider
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
                                          apiClient: ApiClient,
                                          receivePhotosLocalSource: ReceivePhotosLocalSource,
                                          uploadedPhotosLocalSource: UploadPhotosLocalSource,
                                          takenPhotosLocalSource: TakenPhotosLocalSource,
                                          dispatchersProvider: DispatchersProvider): ReceivePhotosRepository {
    return ReceivePhotosRepository(
      database,
      apiClient,
      receivePhotosLocalSource,
      uploadedPhotosLocalSource,
      takenPhotosLocalSource,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideGetUploadedPhotosRepository(uploadedPhotosLocalSource: UploadPhotosLocalSource,
                                              apiClient: ApiClient,
                                              dispatchersProvider: DispatchersProvider): GetUploadedPhotosRepository {
    return GetUploadedPhotosRepository(
      uploadedPhotosLocalSource,
      apiClient,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideGalleryPhotosRepository(database: MyDatabase,
                                          timeUtils: TimeUtils,
                                          dispatchersProvider: DispatchersProvider): GalleryPhotosRepository {
    return GalleryPhotosRepository(
      database,
      timeUtils,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideStorePhotoFromPushNotificationRepository(database: MyDatabase,
                                                           uploadedPhotosRepository: UploadedPhotosRepository,
                                                           receivedPhotosRepository: ReceivedPhotosRepository,
                                                           dispatchersProvider: DispatchersProvider): StorePhotoFromPushNotificationRepository {
    return StorePhotoFromPushNotificationRepository(
      database,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  open fun provideRestoreAccountRepository(database: MyDatabase,
                                           settingsRepository: SettingsRepository,
                                           uploadedPhotosRepository: UploadedPhotosRepository,
                                           receivedPhotosRepository: ReceivedPhotosRepository,
                                           dispatchersProvider: DispatchersProvider): RestoreAccountRepository {
    return RestoreAccountRepository(
      database,
      settingsRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      dispatchersProvider
    )
  }
}