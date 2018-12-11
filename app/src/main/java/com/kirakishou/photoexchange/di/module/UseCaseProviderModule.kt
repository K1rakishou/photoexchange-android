package com.kirakishou.photoexchange.di.module

import android.content.Context
import butterknife.ButterKnife
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.database.source.remote.FirebaseRemoteSource
import com.kirakishou.photoexchange.helper.util.*
import com.kirakishou.photoexchange.interactors.*
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class UseCaseProviderModule {

  @Singleton
  @Provides
  fun provideUploadPhotosUseCase(database: MyDatabase,
                                 apiClient: ApiClient,
                                 timeUtils: TimeUtils,
                                 fileUtils: FileUtils,
                                 bitmapUtils: BitmapUtils,
                                 takenPhotosRepository: TakenPhotosRepository,
                                 uploadedPhotosRepository: UploadedPhotosRepository,
                                 dispatchersProvider: DispatchersProvider): UploadPhotosUseCase {
    return UploadPhotosUseCase(
      database,
      apiClient,
      timeUtils,
      fileUtils,
      bitmapUtils,
      takenPhotosRepository,
      uploadedPhotosRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideFindPhotoAnswersUseCase(database: MyDatabase,
                                     apiClient: ApiClient,
                                     receivedPhotosRepository: ReceivedPhotosRepository,
                                     takenPhotosRepository: TakenPhotosRepository,
                                     uploadedPhotosRepository: UploadedPhotosRepository,
                                     dispatchersProvider: DispatchersProvider): ReceivePhotosUseCase {
    return ReceivePhotosUseCase(
      database,
      apiClient,
      receivedPhotosRepository,
      uploadedPhotosRepository,
      takenPhotosRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideGetGalleryPhotosUseCase(apiClient: ApiClient,
                                     timeUtils: TimeUtils,
                                     pagedApiUtils: PagedApiUtils,
                                     netUtils: NetUtils,
                                     galleryPhotosRepository: GalleryPhotosRepository,
                                     blacklistedPhotoRepository: BlacklistedPhotoRepository,
                                     settingsRepository: SettingsRepository,
                                     dispatchersProvider: DispatchersProvider): GetGalleryPhotosUseCase {
    return GetGalleryPhotosUseCase(
      apiClient,
      timeUtils,
      pagedApiUtils,
      netUtils,
      galleryPhotosRepository,
      blacklistedPhotoRepository,
      settingsRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideReportPhotoUseCase(database: MyDatabase,
                                apiClient: ApiClient,
                                timeUtils: TimeUtils,
                                settingsRepository: SettingsRepository,
                                galleryPhotosRepository: GalleryPhotosRepository,
                                dispatchersProvider: DispatchersProvider): ReportPhotoUseCase {
    return ReportPhotoUseCase(
      database,
      apiClient,
      timeUtils,
      settingsRepository,
      galleryPhotosRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideFavouritePhotoUseCase(database: MyDatabase,
                                   apiClient: ApiClient,
                                   timeUtils: TimeUtils,
                                   settingsRepository: SettingsRepository,
                                   galleryPhotosRepository: GalleryPhotosRepository,
                                   dispatchersProvider: DispatchersProvider): FavouritePhotoUseCase {
    return FavouritePhotoUseCase(
      database,
      apiClient,
      timeUtils,
      settingsRepository,
      galleryPhotosRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideGetUserIdUseCase(settingsRepository: SettingsRepository,
                              apiClient: ApiClient,
                              dispatchersProvider: DispatchersProvider): GetUserIdUseCase {
    return GetUserIdUseCase(
      settingsRepository,
      apiClient,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideGetUploadedPhotosUseCase(apiClient: ApiClient,
                                      pagedApiUtils: PagedApiUtils,
                                      timeUtils: TimeUtils,
                                      settingsRepository: SettingsRepository,
                                      uploadedPhotosRepository: UploadedPhotosRepository,
                                      dispatchersProvider: DispatchersProvider): GetUploadedPhotosUseCase {
    return GetUploadedPhotosUseCase(
      apiClient,
      pagedApiUtils,
      timeUtils,
      settingsRepository,
      uploadedPhotosRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideGetReceivedPhotosUseCase(database: MyDatabase,
                                      apiClient: ApiClient,
                                      timeUtils: TimeUtils,
                                      pagedApiUtils: PagedApiUtils,
                                      uploadedPhotosRepository: UploadedPhotosRepository,
                                      receivedPhotosRepository: ReceivedPhotosRepository,
                                      blacklistedPhotoRepository: BlacklistedPhotoRepository,
                                      settingsRepository: SettingsRepository,
                                      dispatchersProvider: DispatchersProvider): GetReceivedPhotosUseCase {
    return GetReceivedPhotosUseCase(
      database,
      apiClient,
      timeUtils,
      pagedApiUtils,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      blacklistedPhotoRepository,
      settingsRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideRestoreAccountUseCase(database: MyDatabase,
                                   apiClient: ApiClient,
                                   settingsRepository: SettingsRepository,
                                   uploadedPhotosRepository: UploadedPhotosRepository,
                                   receivedPhotosRepository: ReceivedPhotosRepository,
                                   dispatchersProvider: DispatchersProvider): RestoreAccountUseCase {
    return RestoreAccountUseCase(
      database,
      apiClient,
      settingsRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideUpdateFirebaseTokenUseCase(apiClient: ApiClient,
                                        settingsRepository: SettingsRepository,
                                        firebaseRemoteSource: FirebaseRemoteSource,
                                        dispatchersProvider: DispatchersProvider): UpdateFirebaseTokenUseCase {
    return UpdateFirebaseTokenUseCase(
      settingsRepository,
      firebaseRemoteSource,
      apiClient,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideBlacklistPhotoUseCase(database: MyDatabase,
                                   receivedPhotosRepository: ReceivedPhotosRepository,
                                   galleryPhotosRepository: GalleryPhotosRepository,
                                   blacklistedPhotoRepository: BlacklistedPhotoRepository,
                                   dispatchersProvider: DispatchersProvider): BlacklistPhotoUseCase {
    return BlacklistPhotoUseCase(
      database,
      receivedPhotosRepository,
      galleryPhotosRepository,
      blacklistedPhotoRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideGetCurrentLocationUseCase(@Named("app_context") context: Context,
                                       database: MyDatabase,
                                       takenPhotosRepository: TakenPhotosRepository,
                                       settingsRepository: SettingsRepository,
                                       dispatchersProvider: DispatchersProvider): GetCurrentLocationUseCase {
    return GetCurrentLocationUseCase(
      context,
      database,
      takenPhotosRepository,
      settingsRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideStorePhotoFromPushNotificationUseCase(database: MyDatabase,
                                                   receivedPhotosRepository: ReceivedPhotosRepository,
                                                   uploadedPhotosRepository: UploadedPhotosRepository,
                                                   dispatchersProvider: DispatchersProvider): StorePhotoFromPushNotificationUseCase {
    return StorePhotoFromPushNotificationUseCase(
      database,
      receivedPhotosRepository,
      uploadedPhotosRepository,
      dispatchersProvider
    )
  }
}