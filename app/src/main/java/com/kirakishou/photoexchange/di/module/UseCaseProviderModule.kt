package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.api.ApiClientImpl
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
                                   apiClient: ApiClientImpl,
                                   timeUtils: TimeUtils,
                                   bitmapUtils: BitmapUtils,
                                   fileUtils: FileUtils): UploadPhotosUseCase {
        return UploadPhotosUseCase(database, takenPhotosRepository, uploadedPhotosRepository, apiClient, timeUtils, bitmapUtils, fileUtils)
    }

    @Singleton
    @Provides
    fun provideFindPhotoAnswersUseCase(database: MyDatabase,
                                       takenPhotosRepository: TakenPhotosRepository,
                                       receivedPhotosRepository: ReceivedPhotosRepository,
                                       uploadedPhotosRepository: UploadedPhotosRepository,
                                       apiClient: ApiClientImpl): ReceivePhotosUseCase {
        return ReceivePhotosUseCase(database, takenPhotosRepository,
            receivedPhotosRepository, uploadedPhotosRepository, apiClient)
    }

    @Singleton
    @Provides
    fun provideGetGalleryPhotosUseCase(apiClient: ApiClientImpl,
                                       galleryPhotoRepository: GalleryPhotoRepository): GetGalleryPhotosUseCase {
        return GetGalleryPhotosUseCase(apiClient, galleryPhotoRepository)
    }

    @Singleton
    @Provides
    fun provideReportPhotoUseCase(apiClient: ApiClientImpl,
                                  galleryPhotoRepository: GalleryPhotoRepository): ReportPhotoUseCase {
        return ReportPhotoUseCase(apiClient, galleryPhotoRepository)
    }

    @Singleton
    @Provides
    fun provideFavouritePhotoUseCase(apiClient: ApiClientImpl,
                                     database: MyDatabase,
                                     galleryPhotoRepository: GalleryPhotoRepository): FavouritePhotoUseCase {
        return FavouritePhotoUseCase(apiClient, database, galleryPhotoRepository)
    }

    @Singleton
    @Provides
    fun provideGetUserIdUseCase(settingsRepository: SettingsRepository,
                                apiClient: ApiClientImpl): GetUserIdUseCase {
        return GetUserIdUseCase(settingsRepository, apiClient)
    }

    @Singleton
    @Provides
    fun provideGetUploadedPhotosUseCase(uploadedPhotosRepository: UploadedPhotosRepository,
                                        apiClient: ApiClientImpl): GetUploadedPhotosUseCase {
        return GetUploadedPhotosUseCase(uploadedPhotosRepository, apiClient)
    }

    @Singleton
    @Provides
    fun provideGetReceivedPhotosUseCase(database: MyDatabase,
                                        uploadedPhotosRepository: UploadedPhotosRepository,
                                        receivedPhotosRepository: ReceivedPhotosRepository,
                                        apiClient: ApiClientImpl): GetReceivedPhotosUseCase {
        return GetReceivedPhotosUseCase(database, receivedPhotosRepository, uploadedPhotosRepository, apiClient)
    }

    @Singleton
    @Provides
    fun provideGetGalleryPhotosInfoUseCase(apiClient: ApiClientImpl,
                                           galleryPhotoRepository: GalleryPhotoRepository): GetGalleryPhotosInfoUseCase {
        return GetGalleryPhotosInfoUseCase(apiClient, galleryPhotoRepository)
    }
}