package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.AllPhotosActivityModule
import com.kirakishou.photoexchange.di.module.PermissionManagerModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.MyPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/11/2018.
 */

@PerActivity
@Subcomponent(modules = [
    AllPhotosActivityModule::class,
    PermissionManagerModule::class
])
interface AllPhotosActivityComponent {
    fun inject(activity: AllPhotosActivity)
    fun inject(fragment: MyPhotosFragment)
    fun inject(fragment: ReceivedPhotosFragment)
    fun inject(fragment: GalleryFragment)
}