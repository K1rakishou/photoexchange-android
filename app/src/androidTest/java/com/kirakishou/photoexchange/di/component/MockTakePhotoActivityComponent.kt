package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.MockCameraProviderModule
import com.kirakishou.photoexchange.di.module.MockPermissionManagerModule
import com.kirakishou.photoexchange.di.module.MockTakePhotoActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/8/2018.
 */

@PerActivity
@Subcomponent(modules = [
    MockTakePhotoActivityModule::class,
    MockPermissionManagerModule::class,
    MockCameraProviderModule::class])
interface MockTakePhotoActivityComponent {
    fun inject(activity: TakePhotoActivity)

    fun exposePermissionManager(): PermissionManager
    fun exposeCameraProvider(): CameraProvider
}