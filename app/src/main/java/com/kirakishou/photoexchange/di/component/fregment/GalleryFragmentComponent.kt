package com.kirakishou.photoexchange.di.component.fregment

import com.kirakishou.photoexchange.di.module.fragment.GalleryFragmentModule
import com.kirakishou.photoexchange.di.scope.PerFragment
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import dagger.Subcomponent

@PerFragment
@Subcomponent(modules = [
  GalleryFragmentModule::class
])
interface GalleryFragmentComponent {
  fun inject(fragment: GalleryFragment)
}