package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.TakePhotoActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity
import dagger.Component

/**
 * Created by kirakishou on 3/3/2018.
 */
@PerActivity
@Component(modules = [TakePhotoActivityModule::class], dependencies = [ApplicationComponent::class])
interface TakePhotoActivityComponent {
    fun inject(activity: TakePhotoActivity)
}