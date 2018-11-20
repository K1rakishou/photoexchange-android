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
  fun provideUploadPhotosUseCase(database: MyDatabase,
                                 takenPhotosRepository: TakenPhotosRepository,
                                 uploadedPhotosRepository: UploadedPhotosRepository,
                                 apiClient: ApiClient,
                                 timeUtils: TimeUtils,
                                 bitmapUtils: BitmapUtils,
                                 fileUtils: FileUtils): UploadPhotosUseCase {
    return UploadPhotosUseCase(database, takenPhotosRepository, uploadedPhotosRepository,
      apiClient, timeUtils, bitmapUtils, fileUtils)
  }

  @Singleton
  @Provides
  fun provideFindPhotoAnswersUseCase(database: MyDatabase,
                                     takenPhotosRepository: TakenPhotosRepository,
                                     receivedPhotosRepository: ReceivedPhotosRepository,
                                     uploadedPhotosRepository: UploadedPhotosRepository,
                                     apiClient: ApiClient): ReceivePhotosUseCase {
    return ReceivePhotosUseCase(database, takenPhotosRepository,
      receivedPhotosRepository, uploadedPhotosRepository, apiClient)
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
  fun provideReportPhotoUseCase(apiClient: ApiClient,
                                reportPhotoRepository: ReportPhotoRepository,
                                dispatchersProvider: DispatchersProvider): ReportPhotoUseCase {
    return ReportPhotoUseCase(apiClient, reportPhotoRepository, dispatchersProvider)
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
  fun provideGetReceivedPhotosUseCase(database: MyDatabase,
                                      uploadedPhotosRepository: UploadedPhotosRepository,
                                      receivedPhotosRepository: ReceivedPhotosRepository,
                                      apiClient: ApiClient,
                                      timeUtils: TimeUtils,
                                      dispatchersProvider: DispatchersProvider): GetReceivedPhotosUseCase {
    return GetReceivedPhotosUseCase(database, receivedPhotosRepository,
      uploadedPhotosRepository, apiClient, timeUtils, dispatchersProvider)
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