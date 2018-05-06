package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.mvp.viewmodel.factory.TakePhotoActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity
import dagger.Module
import dagger.Provides
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/8/2018.
 */

@Module
class MockTakePhotoActivityModule(
    val mockedView: TakePhotoActivity
) {

    @PerActivity
    @Provides
    fun provideViewModelFactory(schedulerProvider: SchedulerProvider,
                                photosRepository: PhotosRepository,
                                settingsRepository: SettingsRepository,
                                getUserIdUseCase: GetUserIdUseCase): TakePhotoActivityViewModelFactory {
        return TakePhotoActivityViewModelFactory(photosRepository, schedulerProvider, settingsRepository, getUserIdUseCase)
    }
}