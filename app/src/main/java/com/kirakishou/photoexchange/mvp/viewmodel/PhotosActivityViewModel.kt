package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.BlacklistPhotoUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val blacklistPhotoUseCase: BlacklistPhotoUseCase,
  dispatchersProvider: DispatchersProvider
) : BaseViewModel(dispatchersProvider) {
  private val TAG = "PhotosActivityViewModel"

  override fun onCleared() {
    uploadedPhotosFragmentViewModel.clear()
    receivedPhotosFragmentViewModel.clear()
    galleryFragmentViewModel.clear()

    super.onCleared()
  }

  suspend fun checkHasPhotosToUpload(): Boolean {
    return withContext(coroutineContext) {
      takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0
    }
  }

  suspend fun checkCanReceivePhotos(): Boolean {
    return withContext(coroutineContext) {
      val uploadedPhotosCount = uploadedPhotosRepository.count()
      val receivedPhotosCount = receivedPhotosRepository.count()

      return@withContext uploadedPhotosCount > receivedPhotosCount
    }
  }

  fun addReceivedPhoto(photoExchangedData: PhotoExchangedData) {
    Timber.tag(TAG).d("addReceivedPhoto called")

    intercom.tell<UploadedPhotosFragment>()
      .to(UploadedPhotosFragmentEvent.GeneralEvents.OnNewPhotoNotificationReceived(photoExchangedData))
    intercom.tell<ReceivedPhotosFragment>()
      .to(ReceivedPhotosFragmentEvent.GeneralEvents.OnNewPhotoNotificationReceived(photoExchangedData))
  }

  fun deleteAndBlacklistPhoto(photoName: String) {
    launch {
      try {
        blacklistPhotoUseCase.blacklistPhoto(photoName)
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error)

        intercom.tell<PhotosActivity>()
          .to(PhotosActivityEvent.ShowToast("Could not blacklist photo ${photoName}, error message: ${error.message}"))

        return@launch
      }

      intercom.tell<ReceivedPhotosFragment>()
        .to(ReceivedPhotosFragmentEvent.GeneralEvents.RemovePhoto(photoName))
      intercom.tell<GalleryFragment>()
        .to(GalleryFragmentEvent.GeneralEvents.RemovePhoto(photoName))
    }
  }

  suspend fun updateGpsPermissionGranted(granted: Boolean) {
    withContext(coroutineContext) {
      settingsRepository.updateGpsPermissionGranted(granted)
    }
  }

}
