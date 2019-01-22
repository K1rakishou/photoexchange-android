package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.database.source.remote.FirebaseRemoteSource
import com.kirakishou.photoexchange.helper.util.*
import com.kirakishou.photoexchange.usecases.*
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
                                 netUtils: NetUtils,
                                 takenPhotosRepository: TakenPhotosRepository,
                                 uploadedPhotosRepository: UploadedPhotosRepository,
                                 dispatchersProvider: DispatchersProvider): UploadPhotosUseCase {
    return UploadPhotosUseCase(
      database,
      apiClient,
      timeUtils,
      fileUtils,
      bitmapUtils,
      netUtils,
      takenPhotosRepository,
      uploadedPhotosRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideFindPhotoAnswersUseCase(database: MyDatabase,
                                     apiClient: ApiClient,
                                     netUtils: NetUtils,
                                     getPhotoAdditionalInfoUseCase: GetPhotoAdditionalInfoUseCase,
                                     receivedPhotosRepository: ReceivedPhotosRepository,
                                     takenPhotosRepository: TakenPhotosRepository,
                                     uploadedPhotosRepository: UploadedPhotosRepository,
                                     dispatchersProvider: DispatchersProvider): ReceivePhotosUseCase {
    return ReceivePhotosUseCase(
      database,
      apiClient,
      netUtils,
      getPhotoAdditionalInfoUseCase,
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
                                     getFreshPhotosUseCase: GetFreshPhotosUseCase,
                                     getPhotoAdditionalInfoUseCase: GetPhotoAdditionalInfoUseCase,
                                     galleryPhotosRepository: GalleryPhotosRepository,
                                     blacklistedPhotoRepository: BlacklistedPhotoRepository,
                                     settingsRepository: SettingsRepository,
                                     dispatchersProvider: DispatchersProvider): GetGalleryPhotosUseCase {
    return GetGalleryPhotosUseCase(
      apiClient,
      timeUtils,
      pagedApiUtils,
      getFreshPhotosUseCase,
      getPhotoAdditionalInfoUseCase,
      galleryPhotosRepository,
      blacklistedPhotoRepository,
      settingsRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideReportPhotoUseCase(apiClient: ApiClient,
                                netUtils: NetUtils,
                                settingsRepository: SettingsRepository,
                                galleryPhotosRepository: GalleryPhotosRepository,
                                photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
                                dispatchersProvider: DispatchersProvider): ReportPhotoUseCase {
    return ReportPhotoUseCase(
      apiClient,
      netUtils,
      settingsRepository,
      galleryPhotosRepository,
      photoAdditionalInfoRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideFavouritePhotoUseCase(apiClient: ApiClient,
                                   netUtils: NetUtils,
                                   settingsRepository: SettingsRepository,
                                   galleryPhotosRepository: GalleryPhotosRepository,
                                   photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
                                   dispatchersProvider: DispatchersProvider): FavouritePhotoUseCase {
    return FavouritePhotoUseCase(
      apiClient,
      netUtils,
      settingsRepository,
      galleryPhotosRepository,
      photoAdditionalInfoRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideGetUserIdUseCase(apiClient: ApiClient,
                              netUtils: NetUtils,
                              settingsRepository: SettingsRepository,
                              dispatchersProvider: DispatchersProvider): GetUserUuidUseCase {
    return GetUserUuidUseCase(
      apiClient,
      netUtils,
      settingsRepository,
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
                                      getFreshPhotosUseCase: GetFreshPhotosUseCase,
                                      dispatchersProvider: DispatchersProvider): GetUploadedPhotosUseCase {
    return GetUploadedPhotosUseCase(
      apiClient,
      pagedApiUtils,
      timeUtils,
      settingsRepository,
      uploadedPhotosRepository,
      getFreshPhotosUseCase,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideGetReceivedPhotosUseCase(database: MyDatabase,
                                      apiClient: ApiClient,
                                      timeUtils: TimeUtils,
                                      pagedApiUtils: PagedApiUtils,
                                      getFreshPhotosUseCase: GetFreshPhotosUseCase,
                                      getPhotoAdditionalInfoUseCase: GetPhotoAdditionalInfoUseCase,
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
      getFreshPhotosUseCase,
      getPhotoAdditionalInfoUseCase,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      blacklistedPhotoRepository,
      settingsRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideRestoreAccountUseCase(apiClient: ApiClient,
                                   database: MyDatabase,
                                   netUtils: NetUtils,
                                   settingsRepository: SettingsRepository,
                                   uploadedPhotosRepository: UploadedPhotosRepository,
                                   receivedPhotosRepository: ReceivedPhotosRepository,
                                   dispatchersProvider: DispatchersProvider): RestoreAccountUseCase {
    return RestoreAccountUseCase(
      apiClient,
      database,
      netUtils,
      settingsRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideUpdateFirebaseTokenUseCase(apiClient: ApiClient,
                                        netUtils: NetUtils,
                                        settingsRepository: SettingsRepository,
                                        firebaseRemoteSource: FirebaseRemoteSource,
                                        dispatchersProvider: DispatchersProvider): UpdateFirebaseTokenUseCase {
    return UpdateFirebaseTokenUseCase(
      apiClient,
      netUtils,
      settingsRepository,
      firebaseRemoteSource,
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
                                       dispatchersProvider: DispatchersProvider): UpdateNotUploadedPhotosWithCurrentLocation {
    return UpdateNotUploadedPhotosWithCurrentLocation(
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

  @Singleton
  @Provides
  fun provideCheckFirebaseAvailabilityUseCase(firebaseRemoteSource: FirebaseRemoteSource,
                                              settingsRepository: SettingsRepository,
                                              dispatchersProvider: DispatchersProvider): CheckFirebaseAvailabilityUseCase {
    return CheckFirebaseAvailabilityUseCase(
      firebaseRemoteSource,
      settingsRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideTakePhotoUseCase(database: MyDatabase,
                              takenPhotosRepository: TakenPhotosRepository,
                              dispatchersProvider: DispatchersProvider): TakePhotoUseCase {
    return TakePhotoUseCase(
      database,
      takenPhotosRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideGetPhotoAdditionalInfoUseCase(apiClient: ApiClient,
                                           netUtils: NetUtils,
                                           photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
                                           settingsRepository: SettingsRepository,
                                           dispatchersProvider: DispatchersProvider): GetPhotoAdditionalInfoUseCase {
    return GetPhotoAdditionalInfoUseCase(
      apiClient,
      netUtils,
      photoAdditionalInfoRepository,
      settingsRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideGetFreshPhotosUseCase(apiClient: ApiClient,
                                   timeUtils: TimeUtils,
                                   settingsRepository: SettingsRepository,
                                   getPhotoAdditionalInfoUseCase: GetPhotoAdditionalInfoUseCase,
                                   dispatchersProvider: DispatchersProvider): GetFreshPhotosUseCase {
    return GetFreshPhotosUseCase(
      apiClient,
      timeUtils,
      settingsRepository,
      getPhotoAdditionalInfoUseCase,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideCancelPhotoUploadingUseCase(takenPhotosRepository: TakenPhotosRepository,
                                         dispatchersProvider: DispatchersProvider): CancelPhotoUploadingUseCase {
    return CancelPhotoUploadingUseCase(
      takenPhotosRepository,
      dispatchersProvider
    )
  }
}