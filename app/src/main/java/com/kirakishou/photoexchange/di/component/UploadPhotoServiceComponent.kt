package com.kirakishou.photoexchange.di.component

import android.content.Context
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.service.UploadPhotoService
import dagger.Component
import org.greenrobot.eventbus.EventBus
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/5/2017.
 */

@Singleton
@Component(modules = [UploadPhotoServiceModule::class,
    NetworkModule::class,
    GsonModule::class,
    ApiClientModule::class,
    SchedulerProviderModule::class,
    EventBusModule::class,
    DatabaseModule::class,
    MapperModule::class]
)
interface UploadPhotoServiceComponent {
    fun inject(service: UploadPhotoService)

    fun exposeContext(): Context
    fun exposeApiService(): ApiService
    fun exposeApiClient(): ApiClient
    fun exposeSchedulers(): SchedulerProvider
    fun exposeEventBus(): EventBus
    fun exposeDatabase(): MyDatabase
}