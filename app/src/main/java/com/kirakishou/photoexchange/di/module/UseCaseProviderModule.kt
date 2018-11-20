package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
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
  fun provideFindPhotoAnswersUseCase(database: MyDatabase,
                                     receivePhotosRepository: ReceivePhotosRepository,
                                     apiClient: ApiClient): ReceivePhotosUseCase {
    return ReceivePhotosUseCase(database, receivePhotosRepository, apiClient)
  }

  @Singleton
  @Provides
  fun provideGetGalleryPhotosUseCase(getGalleryPhotosRepository: GetGalleryPhotosRepository,
                                     timeUtils: TimeUtils,
                                     dispatchersProvider: DispatchersProvider): GetGalleryPhotosUseCase {
    return GetGalleryPhotosUseCase(getGalleryPhotosRepository, timeUtils, dispatchersProvider)
  }

  @Singleton
  @Provides
  fun provideReportPhotoUseCase(reportPhotoRepository: ReportPhotoRepository,
                                dispatchersProvider: DispatchersProvider): ReportPhotoUseCase {
    return ReportPhotoUseCase(reportPhotoRepository, dispatchersProvider)
  }

  @Singleton
  @Provides
  fun provideFavouritePhotoUseCase(favouritePhotoRepository: FavouritePhotoRepository,
                                   dispatchersProvider: DispatchersProvider): FavouritePhotoUseCase {
    return FavouritePhotoUseCase(favouritePhotoRepository, dispatchersProvider)
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
  fun provideGetUploadedPhotosUseCase(uploadedPhotosRepository: UploadedPhotosRepository,
                                      apiClient: ApiClient,
                                      timeUtils: TimeUtils,
                                      dispatchersProvider: DispatchersProvider): GetUploadedPhotosUseCase {
    return GetUploadedPhotosUseCase(uploadedPhotosRepository, apiClient, timeUtils, dispatchersProvider)
  }

  @Singleton
  @Provides
  fun provideGetReceivedPhotosUseCase(getReceivedPhotosRepository: GetReceivedPhotosRepository,
                                      timeUtils: TimeUtils,
                                      dispatchersProvider: DispatchersProvider): GetReceivedPhotosUseCase {
    return GetReceivedPhotosUseCase(getReceivedPhotosRepository, timeUtils, dispatchersProvider)
  }

  @Singleton
  @Provides
  fun provideRestoreAccountUseCase(apiClient: ApiClient,
                                   database: MyDatabase,
                                   settingsRepository: SettingsRepository,
                                   uploadedPhotosRepository: UploadedPhotosRepository,
                                   receivedPhotosRepository: ReceivedPhotosRepository,
                                   dispatchersProvider: DispatchersProvider): RestoreAccountUseCase {
    return RestoreAccountUseCase(
      apiClient,
      database,
      settingsRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      dispatchersProvider
    )
  }
}