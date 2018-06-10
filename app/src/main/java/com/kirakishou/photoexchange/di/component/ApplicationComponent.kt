package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.PhotoExchangeApplication
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
    DatabaseModule::class,
    GsonModule::class,
    NetworkModule::class,
    DatabaseModule::class,
    ApiClientModule::class,
    ImageLoaderModule::class,
    UseCaseProviderModule::class,
    TimeUtilsModule::class
])
interface ApplicationComponent {
    fun inject(application: PhotoExchangeApplication)

    fun plus(takePhotoActivityModule: TakePhotoActivityModule): TakePhotoActivityComponent
    fun plus(viewTakenPhotoActivityModule: ViewTakenPhotoActivityModule): ViewTakenPhotoActivityComponent
    fun plus(photosActivityModule: PhotosActivityModule): AllPhotosActivityComponent
    fun plus(uploadPhotoServiceModule: UploadPhotoServiceModule): UploadPhotoServiceComponent
    fun plus(receivePhotosServiceModule: ReceivePhotosServiceModule): ReceivePhotosServiceComponent
    fun plus(settingsActivityModule: SettingsActivityModule): SettingsActivityComponent
    fun plus(mapActivityModule: MapActivityModule): MapActivityComponent
}