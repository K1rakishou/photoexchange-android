package com.kirakishou.photoexchange.di.component.activity

import androidx.appcompat.app.AppCompatActivity
import com.kirakishou.photoexchange.di.component.fregment.GalleryFragmentComponent
import com.kirakishou.photoexchange.di.component.fregment.ReceivedPhotosFragmentComponent
import com.kirakishou.photoexchange.di.component.fregment.UploadedPhotosFragmentComponent
import com.kirakishou.photoexchange.di.module.activity.PhotosActivityModule
import com.kirakishou.photoexchange.di.module.fragment.GalleryFragmentModule
import com.kirakishou.photoexchange.di.module.fragment.ReceivedPhotosFragmentModule
import com.kirakishou.photoexchange.di.module.fragment.UploadedPhotosFragmentModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import dagger.Subcomponent

/**
 * Created by kirakishou on 3/11/2018.
 */

@PerActivity
@Subcomponent(modules = [
  PhotosActivityModule::class
])
interface PhotosActivityComponent {
  fun inject(activity: AppCompatActivity)

  fun plus(fragment: UploadedPhotosFragmentModule): UploadedPhotosFragmentComponent
  fun plus(fragment: ReceivedPhotosFragmentModule): ReceivedPhotosFragmentComponent
  fun plus(fragment: GalleryFragmentModule): GalleryFragmentComponent
}