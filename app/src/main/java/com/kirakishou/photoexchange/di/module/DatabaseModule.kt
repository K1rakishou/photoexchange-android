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
import com.kirakishou.photoexchange.helper.util.*
import core.SharedConstants
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
  open fun provideDatabase(@Named("app_context") context: Context): MyDatabase {
    return Room.databaseBuilder(context, MyDatabase::class.java, dbName)
      .build()
  }

  @Singleton
  @Provides
  fun provideFirebaseInstanceId(): FirebaseInstanceId {
    return FirebaseInstanceId.getInstance()
  }

  @Singleton
  @Provides
  @Named("files_directory")
  fun provideFilesDirectoryPath(@Named("app_context") context: Context): String {
    return context.filesDir.absolutePath
  }

  /**
   * Local Sources
   * */

  @Singleton
  @Provides
  open fun provideGalleryPhotoLocalSource(database: MyDatabase,
                                          timeUtils: TimeUtils): GalleryPhotoLocalSource {
    return GalleryPhotoLocalSource(
      database,
      timeUtils,
      SharedConstants.OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL
    )
  }

  @Singleton
  @Provides
  open fun provideGalleryPhotoInfoLocalSource(database: MyDatabase,
                                              timeUtils: TimeUtils): GalleryPhotoInfoLocalSource {
    return GalleryPhotoInfoLocalSource(
      database,
      timeUtils,
      SharedConstants.OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL
    )
  }

  @Singleton
  @Provides
  open fun provideReceivePhotosLocalSource(database: MyDatabase,
                                           timeUtils: TimeUtils): ReceivedPhotosLocalSource {
    return ReceivedPhotosLocalSource(
      database,
      timeUtils,
      SharedConstants.OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL
    )
  }

  @Singleton
  @Provides
  open fun provideUploadPhotosLocalSource(database: MyDatabase,
                                          timeUtils: TimeUtils): UploadedPhotosLocalSource {
    return UploadedPhotosLocalSource(
      database,
      timeUtils
    )
  }

  @Singleton
  @Provides
  open fun provideTempFileLocalSource(database: MyDatabase,
                                      @Named("files_directory") filesDir: String,
                                      timeUtils: TimeUtils,
                                      fileUtils: FileUtils): TempFileLocalSource {
    return TempFileLocalSource(database, filesDir, timeUtils, fileUtils)
  }

  @Singleton
  @Provides
  open fun provideBlacklistedPhotoLocalSource(database: MyDatabase,
                                              timeUtils: TimeUtils): BlacklistedPhotoLocalSource {
    return BlacklistedPhotoLocalSource(
      database,
      timeUtils
    )
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
                                       tempFileLocalSource: TempFileLocalSource): TakenPhotosRepository {
    return TakenPhotosRepository(
      timeUtils,
      database,
      takenPhotosLocalSource,
      tempFileLocalSource
    )
  }

  @Singleton
  @Provides
  open fun provideSettingsRepository(database: MyDatabase): SettingsRepository {
    return SettingsRepository(database)
  }

  @Singleton
  @Provides
  open fun provideReceivedPhotoRepository(database: MyDatabase,
                                          receivedPhotosLocalSource: ReceivedPhotosLocalSource): ReceivedPhotosRepository {
    return ReceivedPhotosRepository(
      database,
      receivedPhotosLocalSource
    )
  }


  @Singleton
  @Provides
  open fun provideUploadedPhotoRepository(database: MyDatabase,
                                          uploadedPhotosLocalSource: UploadedPhotosLocalSource): UploadedPhotosRepository {
    return UploadedPhotosRepository(
      database,
      uploadedPhotosLocalSource
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
  open fun provideGalleryPhotosRepository(database: MyDatabase,
                                          galleryPhotoLocalSource: GalleryPhotoLocalSource,
                                          galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource): GalleryPhotosRepository {
    return GalleryPhotosRepository(
      database,
      galleryPhotoLocalSource,
      galleryPhotoInfoLocalSource
    )
  }

  @Singleton
  @Provides
  open fun provideBlacklistedPhotoRepository(blacklistedPhotoLocalSource: BlacklistedPhotoLocalSource): BlacklistedPhotoRepository {
    return BlacklistedPhotoRepository(
      blacklistedPhotoLocalSource
    )
  }
}