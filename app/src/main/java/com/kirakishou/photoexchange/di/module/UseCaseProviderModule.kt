package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class UseCaseProviderModule {

    @Singleton
    @Provides
    fun provideUploadPhotosUseCase(database: MyDatabase,
                                   myPhotosRepository: PhotosRepository,
                                   apiClient: ApiClient): UploadPhotosUseCase {
        return UploadPhotosUseCase(database, myPhotosRepository, apiClient)
    }

    @Singleton
    @Provides
    fun provideFindPhotoAnswersUseCase(database: MyDatabase,
                                       myPhotosRepository: PhotosRepository,
                                       settingsRepository: SettingsRepository,
                                       photoAnswerRepository: PhotoAnswerRepository,
                                       apiClient: ApiClient): FindPhotoAnswersUseCase {
        return FindPhotoAnswersUseCase(database, myPhotosRepository, settingsRepository, photoAnswerRepository, apiClient)
    }

    @Singleton
    @Provides
    fun provideGetGalleryPhotosUseCase(apiClient: ApiClient): GetGalleryPhotosUseCase {
        return GetGalleryPhotosUseCase(apiClient)
    }

    @Singleton
    @Provides
    fun provideReportPhotoUseCase(apiClient: ApiClient): ReportPhotoUseCase {
        return ReportPhotoUseCase(apiClient)
    }

    @Singleton
    @Provides
    fun provideFavouritePhotoUseCase(apiClient: ApiClient): FavouritePhotoUseCase {
        return FavouritePhotoUseCase(apiClient)
    }
}