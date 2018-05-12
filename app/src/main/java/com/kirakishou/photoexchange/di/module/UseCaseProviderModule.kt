package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
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
                                   apiClient: ApiClient): UploadPhotosUseCase {
        return UploadPhotosUseCase(database, takenPhotosRepository, uploadedPhotosRepository, apiClient)
    }

    @Singleton
    @Provides
    fun provideFindPhotoAnswersUseCase(database: MyDatabase,
                                       takenPhotosRepository: TakenPhotosRepository,
                                       settingsRepository: SettingsRepository,
                                       receivedPhotosRepository: ReceivedPhotosRepository,
                                       apiClient: ApiClient): ReceivePhotosUseCase {
        return ReceivePhotosUseCase(database, takenPhotosRepository, settingsRepository, receivedPhotosRepository, apiClient)
    }

    @Singleton
    @Provides
    fun provideGetGalleryPhotosUseCase(apiClient: ApiClient,
                                       galleryPhotoRepository: GalleryPhotoRepository): GetGalleryPhotosUseCase {
        return GetGalleryPhotosUseCase(apiClient, galleryPhotoRepository)
    }

    @Singleton
    @Provides
    fun provideReportPhotoUseCase(apiClient: ApiClient,
                                  galleryPhotoRepository: GalleryPhotoRepository): ReportPhotoUseCase {
        return ReportPhotoUseCase(apiClient, galleryPhotoRepository)
    }

    @Singleton
    @Provides
    fun provideFavouritePhotoUseCase(apiClient: ApiClient,
                                     database: MyDatabase,
                                     galleryPhotoRepository: GalleryPhotoRepository): FavouritePhotoUseCase {
        return FavouritePhotoUseCase(apiClient, database, galleryPhotoRepository)
    }

    @Singleton
    @Provides
    fun provideGetUserIdUseCase(settingsRepository: SettingsRepository,
                                apiClient: ApiClient): GetUserIdUseCase {
        return GetUserIdUseCase(settingsRepository, apiClient)
    }
}