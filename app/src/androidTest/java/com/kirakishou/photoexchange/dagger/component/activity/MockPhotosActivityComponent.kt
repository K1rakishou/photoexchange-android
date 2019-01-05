package com.kirakishou.photoexchange.dagger.component.activity

import com.kirakishou.photoexchange.dagger.module.activity.MockPhotosActivityModule
import com.kirakishou.photoexchange.dagger.component.fragment.MockUploadedPhotosFragmentComponent
import com.kirakishou.photoexchange.mock.FragmentTestingActivity
import com.kirakishou.photoexchange.di.component.activity.BasePhotosActivityComponent
import com.kirakishou.photoexchange.di.module.fragment.UploadedPhotosFragmentModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [
  MockPhotosActivityModule::class
])
interface MockPhotosActivityComponent : BasePhotosActivityComponent<FragmentTestingActivity> {
  fun plus(fragment: UploadedPhotosFragmentModule): MockUploadedPhotosFragmentComponent
}