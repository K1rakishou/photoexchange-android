package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.rx.scheduler.NormalSchedulers
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/3/2017.
 */

@Module
class SchedulerProviderModule {

    @Singleton
    @Provides
    fun provideSchedulers(): SchedulerProvider {
        return NormalSchedulers()
    }
}