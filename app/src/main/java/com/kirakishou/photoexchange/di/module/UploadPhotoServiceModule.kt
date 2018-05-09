package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.extension.asWeak
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.service.UploadPhotoService
import com.kirakishou.photoexchange.service.UploadPhotoServicePresenter
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/17/2018.
 */

@Module
class UploadPhotoServiceModule(
    val service: UploadPhotoService
) {
    @PerService
    @Provides
    fun provideContext(): Context {
        return service
    }

    @PerService
    @Provides
    fun provideUploadPhotoServicePresenter(myPhotosRepository: PhotosRepository,
                                           settingsRepository: SettingsRepository,
                                           schedulerProvider: SchedulerProvider,
                                           uploadPhotosUseCase: UploadPhotosUseCase,
                                           getUserIdUseCase: GetUserIdUseCase): UploadPhotoServicePresenter {
        return UploadPhotoServicePresenter(service.asWeak(), myPhotosRepository,
            settingsRepository, schedulerProvider, uploadPhotosUseCase, getUserIdUseCase)
    }
}