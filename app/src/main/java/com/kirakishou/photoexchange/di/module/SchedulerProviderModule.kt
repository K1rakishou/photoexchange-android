package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.concurrency.scheduler.NormalSchedulers
import com.kirakishou.photoexchange.helper.concurrency.scheduler.SchedulerProvider
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/3/2018.
 */
@Module
class SchedulerProviderModule {

    @Singleton
    @Provides
    fun provideSchedulers(): SchedulerProvider {
        return NormalSchedulers()
    }
}