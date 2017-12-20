package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.TakePhotoActivityModule
import com.kirakishou.photoexchange.di.module.ViewPhotoFullSizeActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.ViewPhotoFullSizeActivity
import dagger.Component

/**
 * Created by kirakishou on 12/20/2017.
 */

@PerActivity
@Component(modules = [ViewPhotoFullSizeActivityModule::class], dependencies = [ApplicationComponent::class])
interface ViewPhotoFullSizeActivityComponent {
    fun inject(activity: ViewPhotoFullSizeActivity)
}