package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.AllPhotoViewActivity
import com.kirakishou.photoexchange.ui.fragment.SentPhotosListFragment
import dagger.Component

/**
 * Created by kirakishou on 11/7/2017.
 */

@PerActivity
@Component(modules = arrayOf(AllPhotoViewActivityModule::class), dependencies = arrayOf(ApplicationComponent::class))
interface AllPhotoViewActivityComponent {
    fun inject(activity: AllPhotoViewActivity)
    fun inject(fragment: SentPhotosListFragment)
}