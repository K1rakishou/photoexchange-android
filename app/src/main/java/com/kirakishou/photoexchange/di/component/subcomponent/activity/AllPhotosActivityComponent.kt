package com.kirakishou.photoexchange.di.component.subcomponent.activity

import com.kirakishou.photoexchange.di.module.AllPhotosActivityModule
import com.kirakishou.photoexchange.di.module.PermissionManagerModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/11/2018.
 */

@PerActivity
@Subcomponent(modules = [
    AllPhotosActivityModule::class,
    PermissionManagerModule::class])
interface AllPhotosActivityComponent {
    fun inject(activity: AllPhotosActivity)
}