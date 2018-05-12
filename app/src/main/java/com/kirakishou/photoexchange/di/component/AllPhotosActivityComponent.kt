package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.PhotosActivityModule
import com.kirakishou.photoexchange.di.module.PermissionManagerModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/11/2018.
 */

@PerActivity
@Subcomponent(modules = [
    PhotosActivityModule::class,
    PermissionManagerModule::class
])
interface AllPhotosActivityComponent {
    fun inject(activity: PhotosActivity)
    fun inject(fragment: UploadedPhotosFragment)
    fun inject(fragment: ReceivedPhotosFragment)
    fun inject(fragment: GalleryFragment)
}