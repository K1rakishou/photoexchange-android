package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.DatabaseModule
import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.service.PushNotificationReceiverService
import dagger.Subcomponent

@PerService
@Subcomponent(modules = [
  DatabaseModule::class
])
interface PushNotificationReceiverServiceComponent {
  fun inject(service: PushNotificationReceiverService)
}