package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/8/2018.
 */

@Module
class MockSchedulerProviderModule {

    @Singleton
    @Provides
    fun provideSchedulers(): SchedulerProvider {
        return TestSchedulers()
    }

    class TestSchedulers : SchedulerProvider {
        override fun BG(): Scheduler = Schedulers.trampoline()
        override fun UI(): Scheduler = Schedulers.trampoline()
    }
}