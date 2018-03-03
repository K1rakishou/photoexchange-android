package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.concurrency.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.factory.TakePhotoActivityViewModelFactory
import dagger.Module
import dagger.Provides
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/3/2018.
 */

@Module
class TakePhotoActivityModule(val view: WeakReference<TakePhotoActivityView>) {

    @PerActivity
    @Provides
    fun provideViewModelFactory(schedulers: SchedulerProvider,
                                coroutinesPool: CoroutineThreadPoolProvider): TakePhotoActivityViewModelFactory {
        return TakePhotoActivityViewModelFactory(view, coroutinesPool, schedulers)
    }
}