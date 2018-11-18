package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetReceivedPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.ApiErrorException
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import com.kirakishou.photoexchange.ui.viewstate.ReceivedPhotosFragmentViewState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class ReceivedPhotosFragmentViewModel(
  private val settingsRepository: SettingsRepository,
  private val getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val TAG = "ReceivedPhotosFragmentViewModel"

  private val compositeDisposable = CompositeDisposable()
  private val job = Job()

  lateinit var intercom: PhotosActivityViewModelIntercom
  lateinit var actor: SendChannel<Unit>

  val viewState = ReceivedPhotosFragmentViewState()
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

        try {
          intercom.tell<ReceivedPhotosFragment>()
            .to(ReceivedPhotosFragmentEvent.GeneralEvents.PageIsLoading())

          val userId = settingsRepository.getUserId()
          val photos = loadPageOfReceivedPhotos(userId, viewState.lastId, viewState.photosPerPage)

          intercom.tell<ReceivedPhotosFragment>()
            .to(ReceivedPhotosFragmentEvent.GeneralEvents.ShowReceivedPhotos(photos))
        } catch (error: Throwable) {
          if (error is ApiErrorException) {
            knownErrors.onNext(error.errorCode)
          } else {
            unknownErrors.onNext(error)
          }
        }
      }
    }

    launch { actor.send(Unit) }
  }

  fun loadMorePhotos() {
    actor.offer(Unit)
  }

  private suspend fun loadPageOfReceivedPhotos(
    userId: String,
    lastId: Long,
    photosPerPage: Int
  ): List<ReceivedPhoto> {
    if (userId.isEmpty()) {
      return emptyList()
    }

    intercom.tell<ReceivedPhotosFragment>().to(ReceivedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter())

    try {
      val result = getReceivedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage)
      when (result) {
        is Either.Value -> {
          intercom.tell<UploadedPhotosFragment>()
            .to(UploadedPhotosFragmentEvent.GeneralEvents.UpdateReceiverInfo(result.value))
          return result.value
        }
        is Either.Error -> {
          throw result.error
        }
      }
    } finally {
      intercom.tell<ReceivedPhotosFragment>().to(ReceivedPhotosFragmentEvent.GeneralEvents.HideProgressFooter())
    }
  }

  fun onCleared() {
    Timber.tag(TAG).d("onCleared()")

    compositeDisposable.dispose()
    job.cancel()
  }
}