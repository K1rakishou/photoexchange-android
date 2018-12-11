package com.kirakishou.photoexchange.di.component.service

import com.kirakishou.photoexchange.di.module.service.UploadPhotoServiceModule
import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.service.UploadPhotoService
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/17/2018.
 */

@PerService
@Subcomponent(modules = [
  UploadPhotoServiceModule::class
])
interface UploadPhotoServiceComponent {
  fun inject(service: UploadPhotoService)
}