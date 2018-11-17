package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.RxLifecycle
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.interactors.GetGalleryPhotosInfoUseCase
import com.kirakishou.photoexchange.interactors.GetGalleryPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.viewstate.GalleryFragmentViewState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class GalleryFragmentViewModel(
  private val settingsRepository: SettingsRepository,
  private val getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
  private val getGalleryPhotosInfoUseCase: GetGalleryPhotosInfoUseCase,
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val TAG = "GalleryFragmentViewModel"

  private val compositeDisposable = CompositeDisposable()
  private val job = Job()

  lateinit var intercom: PhotosActivityViewModelIntercom

  val viewState = GalleryFragmentViewState()
  val fragmentLifecycle = BehaviorSubject.create<RxLifecycle.FragmentLifecycle>()
  val loadMoreEvent = PublishSubject.create<Unit>()
  val knownErrors = PublishSubject.create<ErrorCode>()
  val unknownErrors = PublishSubject.create<Throwable>()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.CALC()

  init {
    actor<Unit> {
      consumeEach {
        if (!isActive) {
          return@consumeEach
        }

        try {
          intercom.tell<GalleryFragment>()
            .to(GalleryFragmentEvent.GeneralEvents.PageIsLoading())

          val photos = loadNextPageOfGalleryPhotos(viewState.lastUploadedOn, viewState.count)

          intercom.tell<GalleryFragment>()
            .to(GalleryFragmentEvent.GeneralEvents.ShowGalleryPhotos(photos))
        } catch (error: Throwable) {
          if (error is ApiException) {
            knownErrors.onNext(error.errorCode)
          } else {
            unknownErrors.onNext(error)
          }
        }
      }
    }
  }

  private suspend fun loadNextPageOfGalleryPhotos(
    lastUploadedOn: Long,
    count: Int
  ): List<GalleryPhoto> {
    val photos = loadPageOfGalleryPhotos(lastUploadedOn, count)
    val result = getGalleryPhotosInfoUseCase.loadGalleryPhotosInfo(settingsRepository.getUserId(), photos)

    when (result) {
      is Either.Value -> {
        return result.value
      }
      is Either.Error -> {
        throw result.error
      }
    }
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