package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.viewmodel.factory.TakePhotoActivityViewModelFactory
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/3/2018.
 */

@Module
class TakePhotoActivityModule() {

    @PerActivity
    @Provides
    fun provideViewModelFactory(schedulers: SchedulerProvider): TakePhotoActivityViewModelFactory {
        return TakePhotoActivityViewModelFactory(schedulers)
    }
}