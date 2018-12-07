package com.kirakishou.photoexchange.di.component.fregment

import com.kirakishou.photoexchange.di.module.fragment.ReceivedPhotosFragmentModule
import com.kirakishou.photoexchange.di.scope.PerFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import dagger.Subcomponent

@PerFragment
@Subcomponent(modules = [
  ReceivedPhotosFragmentModule::class
])
interface ReceivedPhotosFragmentComponent {
  fun inject(fragment: ReceivedPhotosFragment)
}