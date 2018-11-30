package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import timber.log.Timber

/**
 * Created by kirakishou on 3/11/2018.
 */
class PhotosActivityViewModel(
  val uploadedPhotosFragmentViewModel: UploadedPhotosFragmentViewModel,
  val receivedPhotosFragmentViewModel: ReceivedPhotosFragmentViewModel,
  val galleryFragmentViewModel: GalleryFragmentViewModel,
  val intercom: PhotosActivityViewModelIntercom,
  private val settingsRepository: SettingsRepository,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository
) : BaseViewModel() {
  private val TAG = "PhotosActivityViewModel"

  override fun onCleared() {
    uploadedPhotosFragmentViewModel.clear()
    receivedPhotosFragmentViewModel.clear()
    galleryFragmentViewModel.clear()

    super.onCleared()
  }

  suspend fun checkHasPhotosToUpload(): Boolean {
    return takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0
  }

  suspend fun checkCanReceivePhotos(): Boolean {
    val uploadedPhotosCount = uploadedPhotosRepository.count()
    val receivedPhotosCount = receivedPhotosRepository.count()

    return uploadedPhotosCount > receivedPhotosCount
  }

  fun fetchFreshPhotos() {
    Timber.tag(TAG).d("fetchFreshPhotos called")

    intercom.tell<UploadedPhotosFragment>()
      .to(UploadedPhotosFragmentEvent.GeneralEvents.FetchFreshPhotos)
    intercom.tell<ReceivedPhotosFragment>()
      .to(ReceivedPhotosFragmentEvent.GeneralEvents.FetchFreshPhotos)
  }
}
