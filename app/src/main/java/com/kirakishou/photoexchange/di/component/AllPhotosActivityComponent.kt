package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.AllPhotosActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/11/2018.
 */

@PerActivity
@Subcomponent(modules = [AllPhotosActivityModule::class])
interface AllPhotosActivityComponent {
    fun inject(activity: AllPhotosActivity)
}