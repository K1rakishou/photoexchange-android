package com.kirakishou.photoexchange.di.module

import android.arch.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.AllPhotosActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import dagger.Module
import dagger.Provides
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/11/2018.
 */

@Module
open class AllPhotosActivityModule(
    val activity: AllPhotosActivity
) {

    @PerActivity
    @Provides
    fun provideViewModelFactory(schedulerProvider: SchedulerProvider,
                                photosRepository: PhotosRepository,
                                photoAnswerRepository: PhotoAnswerRepository,
                                settingsRepository: SettingsRepository): AllPhotosActivityViewModelFactory {
        return AllPhotosActivityViewModelFactory(photosRepository, settingsRepository, photoAnswerRepository, schedulerProvider)
    }

    @PerActivity
    @Provides
    fun provideViewModel(viewModelFactory: AllPhotosActivityViewModelFactory): AllPhotosActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(AllPhotosActivityViewModel::class.java)
    }
}