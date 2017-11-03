package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.viewmodel.factory.MainActivityViewModelFactory
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 11/3/2017.
 */

@Module
class MainActivityModule {

    @PerActivity
    @Provides
    fun provideViewModelFactory(apiClient: ApiClient, schedulers: SchedulerProvider): MainActivityViewModelFactory {
        return MainActivityViewModelFactory(apiClient, schedulers)
    }
}