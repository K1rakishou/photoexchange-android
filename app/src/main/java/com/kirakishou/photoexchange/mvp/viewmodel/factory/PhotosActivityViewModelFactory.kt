package com.kirakishou.photoexchange.mvp.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.viewmodel.GalleryFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.ReceivedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.UploadedPhotosFragmentViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 3/11/2018.
 */
class PhotosActivityViewModelFactory
@Inject constructor(
  val settingsRepository: SettingsRepository,
  val takenPhotosRepository: TakenPhotosRepository,
  val uploadedPhotosRepository: UploadedPhotosRepository,
  val receivedPhotosRepository: ReceivedPhotosRepository,
  val uploadedPhotosFragmentViewModel: UploadedPhotosFragmentViewModel,
  val receivedPhotosFragmentViewModel: ReceivedPhotosFragmentViewModel,
  val galleryFragmentViewModel: GalleryFragmentViewModel,
  val reportPhotoUseCase: ReportPhotoUseCase,
  val favouritePhotoUseCase: FavouritePhotoUseCase,
  val schedulerProvider: SchedulerProvider
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    return PhotosActivityViewModel(
      settingsRepository,
      takenPhotosRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      uploadedPhotosFragmentViewModel,
      receivedPhotosFragmentViewModel,
      galleryFragmentViewModel,
      reportPhotoUseCase,
      favouritePhotoUseCase,
      schedulerProvider
    ) as T
  }
}