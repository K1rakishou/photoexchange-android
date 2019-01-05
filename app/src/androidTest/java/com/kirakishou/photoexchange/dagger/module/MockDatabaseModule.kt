package com.kirakishou.photoexchange.dagger.module

import androidx.room.Room
import android.content.Context
import com.google.firebase.iid.FirebaseInstanceId
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.database.source.local.*
import com.kirakishou.photoexchange.helper.database.source.remote.*
import com.kirakishou.photoexchange.helper.util.*
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/4/2018.
 */

@Module
open class MockDatabaseModule {

  @Singleton
  @Provides
  open fun provideDatabase(@Named("app_context") context: Context): MyDatabase {
    return Room.inMemoryDatabaseBuilder(context, MyDatabase::class.java)
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
      Constants.INSERTED_EARLIER_THAN_TIME_DELTA
    )
  }

  @Singleton
  @Provides
  open fun provideReceivePhotosLocalSource(database: MyDatabase,
                                           timeUtils: TimeUtils): ReceivedPhotosLocalSource {
    return ReceivedPhotosLocalSource(
      database,
      timeUtils,
      Constants.INSERTED_EARLIER_THAN_TIME_DELTA
    )
  }

  @Singleton
  @Provides
  open fun providePhotoAdditionalInfoLocalSource(database: MyDatabase,
                                                 timeUtils: TimeUtils): PhotoAdditionalInfoLocalSource {
    return PhotoAdditionalInfoLocalSource(
      database,
      timeUtils,
      Constants.INSERTED_EARLIER_THAN_TIME_DELTA
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
      timeUtils,
      Constants.BLACKLISTED_EARLIER_THAN_TIME_DELTA
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
                                          galleryPhotoLocalSource: GalleryPhotoLocalSource): GalleryPhotosRepository {
    return GalleryPhotosRepository(
      galleryPhotoLocalSource
    )
  }

  @Singleton
  @Provides
  open fun provideBlacklistedPhotoRepository(blacklistedPhotoLocalSource: BlacklistedPhotoLocalSource): BlacklistedPhotoRepository {
    return BlacklistedPhotoRepository(
      blacklistedPhotoLocalSource
    )
  }

  @Singleton
  @Provides
  open fun providePhotoAdditionalInfoRepository(
    photoAdditionalInfoLocalSource: PhotoAdditionalInfoLocalSource
  ): PhotoAdditionalInfoRepository {
    return PhotoAdditionalInfoRepository(
      photoAdditionalInfoLocalSource
    )
  }
}