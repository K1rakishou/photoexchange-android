package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.service.UploadPhotoServicePresenter
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/17/2018.
 */

@Module
class UploadPhotoServicePresenterModule {

    @Singleton
    @Provides
    fun provideUploadPhotoServicePresenter(photosRepository: PhotosRepository,
                                           settingsRepository: SettingsRepository,
                                           coroutinePool: CoroutineThreadPoolProvider): UploadPhotoServicePresenter {
        return UploadPhotoServicePresenter(photosRepository, settingsRepository, coroutinePool)
    }
}