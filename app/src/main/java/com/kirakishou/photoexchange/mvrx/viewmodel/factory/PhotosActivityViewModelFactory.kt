package com.kirakishou.photoexchange.mvrx.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.usecases.BlacklistPhotoUseCase
import com.kirakishou.photoexchange.usecases.CheckFirebaseAvailabilityUseCase
import com.kirakishou.photoexchange.mvrx.viewmodel.GalleryFragmentViewModel
import com.kirakishou.photoexchange.mvrx.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvrx.viewmodel.ReceivedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvrx.viewmodel.UploadedPhotosFragmentViewModel
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
  val netUtils: NetUtils,
  val settingsRepository: SettingsRepository,
  val takenPhotosRepository: TakenPhotosRepository,
  val uploadedPhotosRepository: UploadedPhotosRepository,
  val receivedPhotosRepository: ReceivedPhotosRepository,
  val blacklistPhotoUseCase: BlacklistPhotoUseCase,
  val checkFirebaseAvailabilityUseCase: CheckFirebaseAvailabilityUseCase,
  val dispatchersProvider: DispatchersProvider
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    return PhotosActivityViewModel(
      uploadedPhotosFragmentViewModel,
      receivedPhotosFragmentViewModel,
      galleryFragmentViewModel,
      intercom,
      netUtils,
      settingsRepository,
      takenPhotosRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      blacklistPhotoUseCase,
      checkFirebaseAvailabilityUseCase,
      dispatchersProvider
    ) as T
  }
}