package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import com.kirakishou.photoexchange.ui.fragment.QueuedUpPhotosListFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosListFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosListFragment
import dagger.Component

/**
 * Created by kirakishou on 11/7/2017.
 */

@PerActivity
@Component(modules = arrayOf(AllPhotoViewActivityModule::class), dependencies = arrayOf(ApplicationComponent::class))
interface AllPhotoViewActivityComponent {
    fun inject(activity: AllPhotosViewActivity)
    fun inject(fragment: UploadedPhotosListFragment)
    fun inject(fragment: ReceivedPhotosListFragment)
    fun inject(fragment: QueuedUpPhotosListFragment)
}