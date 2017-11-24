package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.ViewTakenPhotoActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.ViewTakenPhotoActivity
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 11/9/2017.
 */

@Module
class ViewTakenPhotoActivityModule(val activity: ViewTakenPhotoActivity) {

    @PerActivity
    @Provides
    fun provideViewModelFactory(schedulers: SchedulerProvider, takenPhotosRepo: TakenPhotosRepository): ViewTakenPhotoActivityViewModelFactory {
        return ViewTakenPhotoActivityViewModelFactory(schedulers, takenPhotosRepo)
    }
}