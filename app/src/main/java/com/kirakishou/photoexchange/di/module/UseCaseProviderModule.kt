package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.database.source.remote.FirebaseRemoteSource
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.interactors.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class UseCaseProviderModule {

  @Singleton
  @Provides
  fun provideUploadPhotosUseCase(uploadPhotosRepository: UploadPhotosRepository): UploadPhotosUseCase {
    return UploadPhotosUseCase(uploadPhotosRepository)
  }

  @Singleton
  @Provides
  fun provideFindPhotoAnswersUseCase(receivePhotosRepository: ReceivePhotosRepository): ReceivePhotosUseCase {
    return ReceivePhotosUseCase(receivePhotosRepository)
  }

  @Singleton
  @Provides
  fun provideGetGalleryPhotosUseCase(settingsRepository: SettingsRepository,
                                     getGalleryPhotosRepository: GetGalleryPhotosRepository,
                                     timeUtils: TimeUtils,
                                     dispatchersProvider: DispatchersProvider): GetGalleryPhotosUseCase {
    return GetGalleryPhotosUseCase(
      settingsRepository,
      getGalleryPhotosRepository,
      timeUtils,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideReportPhotoUseCase(settingsRepository: SettingsRepository,
                                reportPhotoRepository: ReportPhotoRepository,
                                dispatchersProvider: DispatchersProvider): ReportPhotoUseCase {
    return ReportPhotoUseCase(
      settingsRepository,
      reportPhotoRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideFavouritePhotoUseCase(settingsRepository: SettingsRepository,
                                   favouritePhotoRepository: FavouritePhotoRepository,
                                   dispatchersProvider: DispatchersProvider): FavouritePhotoUseCase {
    return FavouritePhotoUseCase(
      settingsRepository,
      favouritePhotoRepository,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideGetUserIdUseCase(settingsRepository: SettingsRepository,
                              apiClient: ApiClient,
                              dispatchersProvider: DispatchersProvider): GetUserIdUseCase {
    return GetUserIdUseCase(settingsRepository, apiClient, dispatchersProvider)
  }

  @Singleton
  @Provides
  fun provideGetUploadedPhotosUseCase(settingsRepository: SettingsRepository,
                                      getUploadedPhotosRepository: GetUploadedPhotosRepository,
                                      timeUtils: TimeUtils,
                                      dispatchersProvider: DispatchersProvider): GetUploadedPhotosUseCase {
    return GetUploadedPhotosUseCase(
      settingsRepository,
      getUploadedPhotosRepository,
      timeUtils,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideGetReceivedPhotosUseCase(settingsRepository: SettingsRepository,
                                      getReceivedPhotosRepository: GetReceivedPhotosRepository,
                                      timeUtils: TimeUtils,
                                      dispatchersProvider: DispatchersProvider): GetReceivedPhotosUseCase {
    return GetReceivedPhotosUseCase(
      settingsRepository,
      getReceivedPhotosRepository,
      timeUtils,
      dispatchersProvider
    )
  }

  @Singleton
  @Provides
  fun provideRestoreAccountUseCase(apiClient: ApiClient,
                                   restoreAccountRepository: RestoreAccountRepository,
                                   dispatchersProvider: DispatchersProvider): RestoreAccountUseCase {
    return RestoreAccountUseCase(
      apiClient,
      restoreAccountRepository,
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
}