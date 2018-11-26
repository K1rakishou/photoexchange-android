package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.interactors.GetGalleryPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.viewstate.GalleryFragmentViewState
import core.ErrorCode
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

class GalleryFragmentViewModel(
  private val getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val TAG = "GalleryFragmentViewModel"

  private val compositeDisposable = CompositeDisposable()
  private val job = Job()

  lateinit var intercom: PhotosActivityViewModelIntercom
  lateinit var actor: SendChannel<Unit>

  val viewState = GalleryFragmentViewState()
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
          intercom.tell<GalleryFragment>()
            .to(GalleryFragmentEvent.GeneralEvents.PageIsLoading())

          val photos = loadPageOfGalleryPhotos(viewState.getLastUploadedOn(), viewState.count)

          intercom.tell<GalleryFragment>()
            .to(GalleryFragmentEvent.GeneralEvents.ShowGalleryPhotos(photos))
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

  private suspend fun loadPageOfGalleryPhotos(
    lastUploadedOn: Long,
    count: Int
  ): List<GalleryPhoto> {
    intercom.tell<GalleryFragment>().to(GalleryFragmentEvent.GeneralEvents.ShowProgressFooter())

    try {
      val result = getGalleryPhotosUseCase.loadPageOfPhotos(lastUploadedOn, count)
      when (result) {
        is Either.Value -> {
          return result.value
        }
        is Either.Error -> {
          throw result.error
        }
      }
    } finally {
      intercom.tell<GalleryFragment>().to(GalleryFragmentEvent.GeneralEvents.HideProgressFooter())
    }
  }

  fun onCleared() {
    Timber.tag(TAG).d("onCleared()")

    compositeDisposable.dispose()
    job.cancel()
  }
}