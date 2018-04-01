package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
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
}