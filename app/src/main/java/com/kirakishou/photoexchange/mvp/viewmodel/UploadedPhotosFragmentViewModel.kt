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
import com.kirakishou.photoexchange.mvp.model.exception.ApiErrorException
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import com.kirakishou.photoexchange.ui.viewstate.UploadedPhotosFragmentViewState
import core.ErrorCode
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
  private val actor: SendChannel<Unit>

  lateinit var intercom: PhotosActivityViewModelIntercom

  val viewState = UploadedPhotosFragmentViewState()
  val knownErrors = PublishSubject.create<ErrorCode>()
  val unknownErrors = PublishSubject.create<Throwable>()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  init {
    actor = actor {
      consumeEach {
        if (!isActive) {
          return@consumeEach
        }

        when (figureOutWhatPhotosToLoad()) {
          PhotosToLoad.QueuedUpAndFailed -> {
            Timber.tag(TAG).d("Loading queued up and failed photos")
            loadQueuedUpAndFailedPhotos()
          }
          PhotosToLoad.Uploaded -> {
            Timber.tag(TAG).d("Loading uploaded photos")
            loadUploadedPhotos()
          }
        }
      }
    }

    runBlocking { actor.send(Unit) }
  }

  fun loadMorePhotos() {
    actor.offer(Unit)
  }

  private suspend fun loadUploadedPhotos() {
    try {
      val userId = settingsRepository.getUserId()
      val uploadedPhotos = loadPageOfUploadedPhotos(userId, viewState.getLastUploadedOn(), viewState.photosPerPage)

      intercom.tell<UploadedPhotosFragment>()
        .to(UploadedPhotosFragmentEvent.GeneralEvents.ShowUploadedPhotos(uploadedPhotos))
      intercom.tell<PhotosActivity>()
        .to(PhotosActivityEvent.StartReceivingService(PhotosActivityViewModel::class.java,
          "Starting the service after a page of uploaded photos was loaded"))
    } catch (error: Throwable) {
      if (error is ApiErrorException) {
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
          viewState.updateFromUploadedPhotos(result.value)
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
    if (viewState.failedPhotosLoaded) {
      return
    }

    try {
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

      viewState.failedPhotosLoaded = true

      intercom.tell<UploadedPhotosFragment>()
        .to(UploadedPhotosFragmentEvent.GeneralEvents.ShowTakenPhotos(photos))
    } catch (error: Throwable) {
      if (error is ApiErrorException) {
        knownErrors.onNext(error.errorCode)
      } else {
        unknownErrors.onNext(error)
      }
    }
  }

  private suspend fun figureOutWhatPhotosToLoad(): PhotosToLoad {
    val hasFailedToUploadPhotos = takenPhotosRepository.countAllByState(PhotoState.FAILED_TO_UPLOAD) > 0
    val hasQueuedUpPhotos = takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0

    if (hasFailedToUploadPhotos || hasQueuedUpPhotos) {
      return PhotosToLoad.QueuedUpAndFailed
    }

    return PhotosToLoad.Uploaded
  }

  private suspend fun tryToFixStalledPhotos() {
    return takenPhotosRepository.tryToFixStalledPhotos()
  }

  private suspend fun loadFailedToUploadPhotos(): List<TakenPhoto> {
    return takenPhotosRepository.findAllByState(PhotoState.FAILED_TO_UPLOAD)
      .sortedByDescending { it.id }
  }

  private suspend fun loadQueuedUpPhotos(): List<TakenPhoto> {
    return takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP)
      .sortedByDescending { it.id }
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