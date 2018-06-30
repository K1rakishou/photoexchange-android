package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.service.UploadPhotoService
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/17/2018.
 */

@PerService
@Subcomponent(modules = [
    UploadPhotoServiceModule::class,
    LocationServiceModule::class
])
interface UploadPhotoServiceComponent {
    fun inject(service: UploadPhotoService)
}