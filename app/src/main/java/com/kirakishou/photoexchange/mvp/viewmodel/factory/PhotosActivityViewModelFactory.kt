package com.kirakishou.photoexchange.mvp.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.ReportPhotoUseCase
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
  val uploadedPhotosFragmentViewModel: UploadedPhotosFragmentViewModel,
  val receivedPhotosFragmentViewModel: ReceivedPhotosFragmentViewModel,
  val galleryFragmentViewModel: GalleryFragmentViewModel,
  val intercom: PhotosActivityViewModelIntercom,
  val settingsRepository: SettingsRepository,
  val takenPhotosRepository: TakenPhotosRepository,
  val uploadedPhotosRepository: UploadedPhotosRepository,
  val receivedPhotosRepository: ReceivedPhotosRepository
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    return PhotosActivityViewModel(
      uploadedPhotosFragmentViewModel,
      receivedPhotosFragmentViewModel,
      galleryFragmentViewModel,
      intercom,
      settingsRepository,
      takenPhotosRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository
    ) as T
  }
}