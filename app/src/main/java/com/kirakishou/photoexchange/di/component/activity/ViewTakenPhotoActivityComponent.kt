package com.kirakishou.photoexchange.di.component.activity

import com.kirakishou.photoexchange.di.module.activity.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.ViewTakenPhotoActivity
import com.kirakishou.photoexchange.ui.fragment.AddToGalleryDialogFragment
import com.kirakishou.photoexchange.ui.fragment.ViewTakenPhotoFragment
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/9/2018.
 */

@PerActivity
@Subcomponent(modules = [
    ViewTakenPhotoActivityModule::class
])
interface ViewTakenPhotoActivityComponent {
    fun inject(activity: ViewTakenPhotoActivity)
    fun inject(fragment: ViewTakenPhotoFragment)
    fun inject(fragment: AddToGalleryDialogFragment)
}