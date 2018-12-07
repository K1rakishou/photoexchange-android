package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.component.activity.PhotosActivityComponent
import com.kirakishou.photoexchange.di.component.activity.SettingsActivityComponent
import com.kirakishou.photoexchange.di.component.activity.TakePhotoActivityComponent
import com.kirakishou.photoexchange.di.component.activity.ViewTakenPhotoActivityComponent
import com.kirakishou.photoexchange.di.component.service.ReceivePhotosServiceComponent
import com.kirakishou.photoexchange.di.component.service.UploadPhotoServiceComponent
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.di.module.activity.PhotosActivityModule
import com.kirakishou.photoexchange.di.module.activity.SettingsActivityModule
import com.kirakishou.photoexchange.di.module.activity.TakePhotoActivityModule
import com.kirakishou.photoexchange.di.module.activity.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.di.module.service.ReceivePhotosServiceModule
import com.kirakishou.photoexchange.di.module.service.UploadPhotoServiceModule
import com.kirakishou.photoexchange.service.PushNotificationReceiverService
import dagger.Component
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/3/2018.
 */

@Singleton
@Component(modules = [
  ApplicationModule::class,
  SchedulerProviderModule::class,
  DispatchersProviderModule::class,
  DatabaseModule::class,
  GsonModule::class,
  NetworkModule::class,
  DatabaseModule::class,
  ApiClientModule::class,
  ImageLoaderModule::class,
  UseCaseProviderModule::class,
  UtilsModule::class
])
interface ApplicationComponent {
  fun inject(application: PhotoExchangeApplication)
  fun inject(pushNotificationReceiverService: PushNotificationReceiverService)

  fun plus(takePhotoActivityModule: TakePhotoActivityModule): TakePhotoActivityComponent
  fun plus(viewTakenPhotoActivityModule: ViewTakenPhotoActivityModule): ViewTakenPhotoActivityComponent
  fun plus(photosActivityModule: PhotosActivityModule): PhotosActivityComponent
  fun plus(uploadPhotoServiceModule: UploadPhotoServiceModule): UploadPhotoServiceComponent
  fun plus(receivePhotosServiceModule: ReceivePhotosServiceModule): ReceivePhotosServiceComponent
  fun plus(settingsActivityModule: SettingsActivityModule): SettingsActivityComponent
}