package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.component.subcomponent.application.AllPhotosActivityComponent
import com.kirakishou.photoexchange.di.component.subcomponent.application.TakePhotoActivityComponent
import com.kirakishou.photoexchange.di.component.subcomponent.application.ViewTakenPhotoActivityComponent
import com.kirakishou.photoexchange.di.module.*
import dagger.Component
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/3/2018.
 */

@Singleton
@Component(modules = [
    ApplicationModule::class,
    SchedulerProviderModule::class,
    CoroutineThreadPoolProviderModule::class,
    DatabaseModule::class,
    GsonModule::class,
    NetworkModule::class,
    DatabaseModule::class,
    ApiClientModule::class
])
interface ApplicationComponent {
    fun inject(application: PhotoExchangeApplication)

    fun plus(takePhotoActivityModule: TakePhotoActivityModule): TakePhotoActivityComponent
    fun plus(viewTakenPhotoActivityModule: ViewTakenPhotoActivityModule): ViewTakenPhotoActivityComponent
    fun plus(allPhotosActivityModule: AllPhotosActivityModule): AllPhotosActivityComponent
}