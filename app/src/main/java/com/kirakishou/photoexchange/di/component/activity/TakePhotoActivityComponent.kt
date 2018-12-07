package com.kirakishou.photoexchange.di.component.activity

import com.kirakishou.photoexchange.di.module.activity.TakePhotoActivityModule
import com.kirakishou.photoexchange.di.module.VibratorModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/3/2018.
 */
@PerActivity
@Subcomponent(modules = [
  TakePhotoActivityModule::class,
  VibratorModule::class
])
interface TakePhotoActivityComponent {
  fun inject(activity: TakePhotoActivity)
}