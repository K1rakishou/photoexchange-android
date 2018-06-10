package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.MapActivityModule
import com.kirakishou.photoexchange.di.module.PermissionManagerModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.MapActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [
    MapActivityModule::class,
    PermissionManagerModule::class
])
interface MapActivityComponent {
    fun inject(activity: MapActivity)
}