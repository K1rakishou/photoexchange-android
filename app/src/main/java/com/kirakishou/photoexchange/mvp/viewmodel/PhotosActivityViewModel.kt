package com.kirakishou.photoexchange.mvp.viewmodel

import android.annotation.SuppressLint
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
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.interactors.BlacklistPhotoUseCase
import com.kirakishou.photoexchange.interactors.CheckFirebaseAvailabilityUseCase
import com.kirakishou.photoexchange.mvp.model.NewReceivedPhoto
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
  private val netUtils: NetUtils,
  private val settingsRepository: SettingsRepository,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val blacklistPhotoUseCase: BlacklistPhotoUseCase,
  private val checkFirebaseAvailabilityUseCase: CheckFirebaseAvailabilityUseCase,
  dispatchersProvider: DispatchersProvider
) : BaseViewModel(dispatchersProvider) {
  private val TAG = "PhotosActivityViewModel"

  override fun onCleared() {
    uploadedPhotosFragmentViewModel.clear()
    receivedPhotosFragmentViewModel.clear()
    galleryFragmentViewModel.clear()

    super.onCleared()
  }

  suspend fun checkCanUploadPhotos(): CanUploadPhotoResult {
    return withContext(coroutineContext) {
      if (!netUtils.canLoadImages()) {
        return@withContext CanUploadPhotoResult.PhotoUploadingDisabled
      }

      val count = takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP)
      Timber.tag(TAG).d("Queued up photo count = $count")

      if (count > 0) {
        return@withContext CanUploadPhotoResult.HasQueuedUpPhotos
      }

      return@withContext CanUploadPhotoResult.HasNoQueuedUpPhotos
    }
  }

  @SuppressLint("BinaryOperationInTimber")
  suspend fun checkCanReceivePhotos(): CanReceivePhotoResult {
    return withContext(coroutineContext) {
      if (!netUtils.canAccessNetwork()) {
        return@withContext CanReceivePhotoResult.NetworkAccessDisabled
      }

      val uploadedPhotosCount = uploadedPhotosRepository.count()
      val receivedPhotosCount = receivedPhotosRepository.count()

      Timber.tag(TAG).d(
        "uploadedPhotosCount = $uploadedPhotosCount, " +
          "receivedPhotosCount = $receivedPhotosCount, " +
          "uploadedPhotosCount > receivedPhotosCount = ${uploadedPhotosCount > receivedPhotosCount}"
      )

      if (uploadedPhotosCount > receivedPhotosCount) {
        return@withContext CanReceivePhotoResult.HasMoreUploadedPhotosThanReceived
      }

      return@withContext CanReceivePhotoResult.HasLessOrEqualUploadedPhotosThanReceived
    }
  }

  /**
   * When we receive a new photo information via the push notification this method will send that
   * information to both UploadedPhotosFragment and ReceivedPhotosFragment so they can internally add
   * that photo to their inner lists of photos
   * */
  fun addReceivedPhoto(newReceivedPhoto: NewReceivedPhoto) {
    Timber.tag(TAG).d("addReceivedPhoto called")

    val list = listOf(newReceivedPhoto)

    intercom.tell<UploadedPhotosFragment>()
      .to(UploadedPhotosFragmentEvent.GeneralEvents.OnNewPhotosReceived(list))
    intercom.tell<ReceivedPhotosFragment>()
      .to(ReceivedPhotosFragmentEvent.GeneralEvents.OnNewPhotosReceived(list))
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

  suspend fun checkFirebaseAvailability(): CheckFirebaseAvailabilityUseCase.FirebaseAvailabilityResult {
    return withContext(coroutineContext) {
      return@withContext checkFirebaseAvailabilityUseCase.check()
    }
  }

  enum class CanUploadPhotoResult {
    HasQueuedUpPhotos,
    HasNoQueuedUpPhotos,
    PhotoUploadingDisabled
  }

  enum class CanReceivePhotoResult {
    HasMoreUploadedPhotosThanReceived,
    HasLessOrEqualUploadedPhotosThanReceived,
    NetworkAccessDisabled
  }
}
