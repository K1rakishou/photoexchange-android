package com.kirakishou.photoexchange.di.component

import android.content.Context
import com.kirakishou.photoexchange.di.module.ApplicationModule
import com.kirakishou.photoexchange.di.module.CoroutineThreadPoolProviderModule
import com.kirakishou.photoexchange.di.module.SchedulerProviderModule
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.concurrency.scheduler.SchedulerProvider
import dagger.Component
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/3/2018.
 */

@Singleton
@Component(modules = [
    ApplicationModule::class,
    SchedulerProviderModule::class,
    CoroutineThreadPoolProviderModule::class])
interface ApplicationComponent {
    fun exposeContext(): Context
    fun exposeSchedulers(): SchedulerProvider
    fun exposeCoroutineThreadPoolProvider(): CoroutineThreadPoolProvider
}