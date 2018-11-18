package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.ReportPhotoUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.Constants
import io.reactivex.Completable
import io.reactivex.Observable
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Created by kirakishou on 3/11/2018.
 */
class PhotosActivityViewModel(
  private val settingsRepository: SettingsRepository,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  val uploadedPhotosFragmentViewModel: UploadedPhotosFragmentViewModel,
  val receivedPhotosFragmentViewModel: ReceivedPhotosFragmentViewModel,
  val galleryFragmentViewModel: GalleryFragmentViewModel,
  private val reportPhotoUseCase: ReportPhotoUseCase,
  private val favouritePhotoUseCase: FavouritePhotoUseCase,
  private val schedulerProvider: SchedulerProvider
) : BaseViewModel() {

  private val TAG = "PhotosActivityViewModel"

  val intercom = PhotosActivityViewModelIntercom()

  init {
    uploadedPhotosFragmentViewModel.intercom = intercom
    receivedPhotosFragmentViewModel.intercom = intercom
    galleryFragmentViewModel.intercom = intercom
  }

  override fun onCleared() {
    Timber.tag(TAG).d("onCleared()")

    uploadedPhotosFragmentViewModel.onCleared()
    receivedPhotosFragmentViewModel.onCleared()
    galleryFragmentViewModel.onCleared()

    super.onCleared()
  }

  suspend fun reportPhoto(photoName: String): Either<Exception, Boolean> {
    return withContext(coroutineContext) {
      val userId = settingsRepository.getUserIdOrThrow()
      return@withContext reportPhotoUseCase.reportPhoto(userId, photoName)
    }
  }

  suspend fun favouritePhoto(photoName: String): Either<Exception, FavouritePhotoUseCase.FavouritePhotoResult> {
    return withContext(coroutineContext) {
      val userId = settingsRepository.getUserIdOrThrow()
      return@withContext favouritePhotoUseCase.favouritePhoto(userId, photoName)
    }
  }

  suspend fun checkHasPhotosToUpload(): Boolean {
    return takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0
  }

  fun checkHasPhotosToReceive(): Observable<Boolean> {
    return Observable.fromCallable {
      val uploadedPhotosCount = uploadedPhotosRepository.count()
      val receivedPhotosCount = receivedPhotosRepository.count()

      return@fromCallable uploadedPhotosCount > receivedPhotosCount
    }.subscribeOn(schedulerProvider.IO())
  }

  fun deletePhotoById(photoId: Long): Completable {
    return Completable.fromAction {
      takenPhotosRepository.deletePhotoById(photoId)
      if (Constants.isDebugBuild) {
        check(takenPhotosRepository.findById(photoId).isEmpty())
      }
    }.subscribeOn(schedulerProvider.IO())
  }

  fun changePhotoState(photoId: Long, newPhotoState: PhotoState): Completable {
    return Completable.fromAction {
      takenPhotosRepository.updatePhotoState(photoId, newPhotoState)
    }.subscribeOn(schedulerProvider.IO())
  }

  suspend fun updateGpsPermissionGranted(granted: Boolean) {
    settingsRepository.updateGpsPermissionGranted(granted)
  }
}
