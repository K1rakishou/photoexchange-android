package com.kirakishou.photoexchange.di.component.service

import com.kirakishou.photoexchange.di.module.service.ReceivePhotosServiceModule
import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.service.ReceivePhotosService
import dagger.Subcomponent


@PerService
@Subcomponent(modules = [
    ReceivePhotosServiceModule::class
])
interface ReceivePhotosServiceComponent {
    fun inject(service: ReceivePhotosService)
}