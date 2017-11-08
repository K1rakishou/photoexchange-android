package com.kirakishou.photoexchange.di.component

import android.content.Context
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.service.SendPhotoService
import dagger.Component
import org.greenrobot.eventbus.EventBus
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/5/2017.
 */

@Singleton
@Component(modules = arrayOf(
        ServiceModule::class,
        NetworkModule::class,
        GsonModule::class,
        ApiClientModule::class,
        SchedulerProviderModule::class,
        EventBusModule::class
))
interface ServiceComponent {
    fun inject(service: SendPhotoService)

    fun exposeContext(): Context
    fun exposeApiService(): ApiService
    fun exposeApiClient(): ApiClient
    fun exposeSchedulers(): SchedulerProvider
    fun exposeEventBus(): EventBus
}