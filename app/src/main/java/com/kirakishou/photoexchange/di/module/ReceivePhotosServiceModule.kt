package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.interactors.ReceivePhotosUseCase
import com.kirakishou.photoexchange.service.ReceivePhotosServicePresenter
import dagger.Module
import dagger.Provides

@Module
class ReceivePhotosServiceModule() {

    @PerService
    @Provides
    fun provideReceivePhotosServicePresenter(uploadedPhotosRepository: UploadedPhotosRepository,
                                             settingsRepository: SettingsRepository,
                                             schedulerProvider: SchedulerProvider,
                                             receivePhotosUseCase: ReceivePhotosUseCase): ReceivePhotosServicePresenter {
        return ReceivePhotosServicePresenter(uploadedPhotosRepository,
            settingsRepository, schedulerProvider, receivePhotosUseCase)
    }
}