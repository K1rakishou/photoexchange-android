package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.FindPhotoAnswerServiceModule
import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.service.FindPhotoAnswerService
import dagger.Subcomponent


@PerService
@Subcomponent(modules = [
    FindPhotoAnswerServiceModule::class
])
interface FindPhotoAnswerServiceComponent {
    fun inject(service: FindPhotoAnswerService)
}