package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.extension.asWeak
import com.kirakishou.photoexchange.interactors.FindPhotoAnswersUseCase
import com.kirakishou.photoexchange.service.FindPhotoAnswerService
import com.kirakishou.photoexchange.service.FindPhotoAnswerServicePresenter
import dagger.Module
import dagger.Provides

@Module
class FindPhotoAnswerServiceModule(
    val service: FindPhotoAnswerService
) {
    @PerService
    @Provides
    fun provideContext(): Context {
        return service
    }

    @PerService
    @Provides
    fun provideFindPhotoAnswerServicePresenter(myPhotosRepository: PhotosRepository,
                                               settingsRepository: SettingsRepository,
                                               schedulerProvider: SchedulerProvider,
                                               findPhotoAnswersUseCase: FindPhotoAnswersUseCase): FindPhotoAnswerServicePresenter {
        return FindPhotoAnswerServicePresenter(service.asWeak(), myPhotosRepository, settingsRepository,
            schedulerProvider, findPhotoAnswersUseCase)
    }
}