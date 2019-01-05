package com.kirakishou.photoexchange.dagger.component.fragment

import com.kirakishou.photoexchange.di.module.fragment.UploadedPhotosFragmentModule
import com.kirakishou.photoexchange.di.scope.PerFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import dagger.Subcomponent

@PerFragment
@Subcomponent(modules = [
  UploadedPhotosFragmentModule::class
])
interface MockUploadedPhotosFragmentComponent {
  fun inject(fragment: UploadedPhotosFragment)
}