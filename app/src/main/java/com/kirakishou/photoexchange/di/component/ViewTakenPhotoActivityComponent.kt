package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.ViewTakenPhotoActivity
import dagger.Component

/**
 * Created by kirakishou on 11/9/2017.
 */

@PerActivity
@Component(modules = arrayOf(ViewTakenPhotoActivityModule::class), dependencies = arrayOf(ApplicationComponent::class))
interface ViewTakenPhotoActivityComponent {
    fun inject(activity: ViewTakenPhotoActivity)
}