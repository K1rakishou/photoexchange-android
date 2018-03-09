package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.CameraProviderModule
import com.kirakishou.photoexchange.di.module.PermissionManagerModule
import com.kirakishou.photoexchange.di.module.TakePhotoActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/3/2018.
 */
@PerActivity
@Subcomponent(modules = [
    TakePhotoActivityModule::class,
    PermissionManagerModule::class,
    CameraProviderModule::class])
interface TakePhotoActivityComponent {
    fun inject(activity: TakePhotoActivity)
}