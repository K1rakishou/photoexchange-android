package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.extension.asWeak
import com.kirakishou.photoexchange.interactors.ReceivePhotosUseCase
import com.kirakishou.photoexchange.service.ReceivePhotosService
import com.kirakishou.photoexchange.service.ReceivePhotosServicePresenter
import dagger.Module
import dagger.Provides

@Module
class ReceivePhotosServiceModule(
    val service: ReceivePhotosService
) {
    @PerService
    @Provides
    fun provideContext(): Context {
        return service
    }

    @PerService
    @Provides
    fun provideReceivePhotosServicePresenter(myPhotosRepository: PhotosRepository,
                                             settingsRepository: SettingsRepository,
                                             schedulerProvider: SchedulerProvider,
                                             receivePhotosUseCase: ReceivePhotosUseCase): ReceivePhotosServicePresenter {
        return ReceivePhotosServicePresenter(service.asWeak(), myPhotosRepository, settingsRepository,
            schedulerProvider, receivePhotosUseCase)
    }
}