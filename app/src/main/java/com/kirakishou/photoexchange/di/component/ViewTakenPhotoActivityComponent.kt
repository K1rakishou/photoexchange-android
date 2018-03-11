package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.ImageLoaderModule
import com.kirakishou.photoexchange.di.module.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.ViewTakenPhotoActivity
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/9/2018.
 */

@PerActivity
@Subcomponent(modules = [ViewTakenPhotoActivityModule::class])
interface ViewTakenPhotoActivityComponent {
    fun inject(activity: ViewTakenPhotoActivity)
}