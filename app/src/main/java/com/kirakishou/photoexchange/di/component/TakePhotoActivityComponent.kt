package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.TakePhotoActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.service.SendPhotoService
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity
import dagger.Component

/**
 * Created by kirakishou on 11/3/2017.
 */

@PerActivity
@Component(modules = arrayOf(TakePhotoActivityModule::class), dependencies = arrayOf(ApplicationComponent::class))
interface TakePhotoActivityComponent {
    fun inject(activity: TakePhotoActivity)
}