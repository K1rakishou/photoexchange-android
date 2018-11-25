package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.response.FavouritePhotoResponseData
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.ReportPhotoUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
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
  private val reportPhotoUseCase: ReportPhotoUseCase,
  private val favouritePhotoUseCase: FavouritePhotoUseCase
) : BaseViewModel() {

  private val TAG = "PhotosActivityViewModel"

  init {
    galleryFragmentViewModel.intercom = intercom
  }

  override fun onCleared() {
    Timber.tag(TAG).d("onCleared()")

    uploadedPhotosFragmentViewModel.clear()
    receivedPhotosFragmentViewModel.clear()
    galleryFragmentViewModel.onCleared()

    super.onCleared()
  }

  suspend fun reportPhoto(photoName: String): Either<Exception, Boolean> {
    return withContext(coroutineContext) {
      val userId = settingsRepository.getUserId()
      if (userId.isEmpty()) {
        throw EmptyUserIdException()
      }

      return@withContext reportPhotoUseCase.reportPhoto(userId, photoName)
    }
  }

  suspend fun favouritePhoto(photoName: String): Either<Exception, FavouritePhotoResponseData> {
    return withContext(coroutineContext) {
      val userId = settingsRepository.getUserId()
      if (userId.isEmpty()) {
        throw EmptyUserIdException()
      }

      return@withContext favouritePhotoUseCase.favouritePhoto(userId, photoName)
    }
  }

  suspend fun checkHasPhotosToUpload(): Boolean {
    return takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0
  }

  suspend fun checkCanReceivePhotos(): Boolean {
    val uploadedPhotosCount = uploadedPhotosRepository.count()
    val receivedPhotosCount = receivedPhotosRepository.count()

    return uploadedPhotosCount > receivedPhotosCount
  }

  suspend fun updateGpsPermissionGranted(granted: Boolean) {
    settingsRepository.updateGpsPermissionGranted(granted)
  }
}
