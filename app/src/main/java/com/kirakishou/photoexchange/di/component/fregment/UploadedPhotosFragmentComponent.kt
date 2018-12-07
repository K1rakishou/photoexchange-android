package com.kirakishou.photoexchange.di.component.fregment

import com.kirakishou.photoexchange.di.module.fragment.UploadedPhotosFragmentModule
import com.kirakishou.photoexchange.di.scope.PerFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import dagger.Subcomponent

@PerFragment
@Subcomponent(modules = [
  UploadedPhotosFragmentModule::class
])
interface UploadedPhotosFragmentComponent {
  fun inject(fragment: UploadedPhotosFragment)
}