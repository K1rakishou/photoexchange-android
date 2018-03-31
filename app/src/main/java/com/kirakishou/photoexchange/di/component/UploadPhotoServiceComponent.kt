package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.service.UploadPhotoService
import dagger.Component
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/17/2018.
 */

@Singleton
@Component(modules = [
    UploadPhotoServiceModule::class,
    SchedulerProviderModule::class,
    UploadPhotoServicePresenterModule::class,
    GsonModule::class,
    NetworkModule::class,
    DatabaseModule::class,
    ApiClientModule::class
])
interface UploadPhotoServiceComponent {
    fun inject(service: UploadPhotoService)
}