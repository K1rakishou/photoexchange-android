package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUploadedPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import com.kirakishou.photoexchange.ui.viewstate.UploadedPhotosFragmentViewState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class UploadedPhotosFragmentViewModel(
  private val takenPhotosRepository: TakenPhotosRepository,
  private val settingsRepository: SettingsRepository,
  private val getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val TAG = "UploadedPhotosFragmentViewModel"

  private val compositeDisposable = CompositeDisposable()
  private val job = Job()

  lateinit var intercom: PhotosActivityViewModelIntercom
  lateinit var actor: SendChannel<Unit>

  val viewState = UploadedPhotosFragmentViewState()
  val knownErrors = PublishSubject.create<ErrorCode>()
  val unknownErrors = PublishSubject.create<Throwable>()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  init {
    actor = actor(capacity = 1) {
      consumeEach {
        if (!isActive) {
          return@consumeEach
        }

        when (figureOutWhatPhotosToLoad()) {
          PhotosToLoad.QueuedUpAndFailed -> {
            loadQueuedUpAndFailedPhotos()
          }
          PhotosToLoad.Uploaded -> {
            loadUploadedPhotos()
          }
        }
      }
    }

    launch { actor.send(Unit) }
  }

  fun loadMorePhotos() {
    actor.offer(Unit)
  }

  private suspend fun loadUploadedPhotos() {
    try {
      intercom.tell<UploadedPhotosFragment>()
        .to(UploadedPhotosFragmentEvent.GeneralEvents.EnableEndlessScrolling())

      intercom.tell<UploadedPhotosFragment>()
        .to(UploadedPhotosFragmentEvent.GeneralEvents.PageIsLoading())

      val userId = settingsRepository.getUserId()
      val uploadedPhotos = loadPageOfUploadedPhotos(userId, viewState.lastId, viewState.photosPerPage)

      intercom.tell<UploadedPhotosFragment>()
        .to(UploadedPhotosFragmentEvent.GeneralEvents.ShowUploadedPhotos(uploadedPhotos))
      intercom.tell<PhotosActivity>()
        .to(PhotosActivityEvent.StartReceivingService(PhotosActivityViewModel::class.java,
          "Starting the service after a page of uploaded photos was loaded"))
    } catch (error: Throwable) {
      if (error is ApiException) {
        knownErrors.onNext(error.errorCode)
      } else {
        unknownErrors.onNext(error)
      }
    }
  }

  private suspend fun loadPageOfUploadedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<UploadedPhoto> {
    if (userId.isEmpty()) {
      return emptyList()
    }

    intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter())

    try {
      val result = getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastUploadedOn, count)
      when (result) {
        is Either.Value -> {
          return result.value
        }
        is Either.Error -> {
          throw result.error
        }
      }
    } finally {
      intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.GeneralEvents.HideProgressFooter())
    }
  }

  private suspend fun loadQueuedUpAndFailedPhotos() {
    try {
      intercom.tell<UploadedPhotosFragment>()
        .to(UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling())

      tryToFixStalledPhotos()

      val queuedUpPhotos = loadQueuedUpPhotos()
      val failedToUploadPhotos = loadFailedToUploadPhotos()

      if (queuedUpPhotos.isNotEmpty()) {
        intercom.tell<PhotosActivity>()
          .to(PhotosActivityEvent.StartUploadingService(PhotosActivityViewModel::class.java,
            "There are queued up photos in the database"))
      }

      val photos = mutableListOf<TakenPhoto>()
      photos.addAll(failedToUploadPhotos)
      photos.addAll(queuedUpPhotos)

      intercom.tell<UploadedPhotosFragment>()
        .to(UploadedPhotosFragmentEvent.GeneralEvents.ShowTakenPhotos(photos))
    } catch (error: Throwable) {
      if (error is ApiException) {
        knownErrors.onNext(error.errorCode)
      } else {
        unknownErrors.onNext(error)
      }
    }
  }

  private suspend fun figureOutWhatPhotosToLoad(): PhotosToLoad {
    return withContext(dispatchersProvider.DISK()) {
      val hasFailedToUploadPhotos = takenPhotosRepository.countAllByState(PhotoState.FAILED_TO_UPLOAD) > 0
      val hasQueuedUpPhotos = takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0

      if (hasFailedToUploadPhotos || hasQueuedUpPhotos) {
        return@withContext PhotosToLoad.QueuedUpAndFailed
      }

      return@withContext PhotosToLoad.Uploaded
    }
  }

  private suspend fun tryToFixStalledPhotos() {
    return withContext(dispatchersProvider.DISK()) {
      return@withContext takenPhotosRepository.tryToFixStalledPhotos()
    }
  }

  private suspend fun loadFailedToUploadPhotos(): List<TakenPhoto> {
    return withContext(dispatchersProvider.DISK()) {
      return@withContext takenPhotosRepository.findAllByState(PhotoState.FAILED_TO_UPLOAD)
        .sortedBy { it.id }
    }
  }

  private suspend fun loadQueuedUpPhotos(): List<TakenPhoto> {
    return withContext(dispatchersProvider.DISK()) {
      return@withContext takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP)
        .sortedBy { it.id }
    }
  }

  fun onCleared() {
    Timber.tag(TAG).d("onCleared()")

    compositeDisposable.dispose()
    job.cancel()
  }

  enum class PhotosToLoad {
    QueuedUpAndFailed,
    Uploaded
  }
}