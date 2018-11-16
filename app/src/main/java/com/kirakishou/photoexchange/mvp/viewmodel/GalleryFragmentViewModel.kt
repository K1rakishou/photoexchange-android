package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.RxLifecycle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
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
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.rx2.rxObservable
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class GalleryFragmentViewModel(
  private val imageLoader: ImageLoader,
  private val settingsRepository: SettingsRepository,
  private val getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
  private val getGalleryPhotosInfoUseCase: GetGalleryPhotosInfoUseCase,
  private val schedulerProvider: SchedulerProvider,
  private val adapterLoadMoreItemsDelayMs: Long,
  private val progressFooterRemoveDelayMs: Long
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
    get() = job

  init {
    compositeDisposable += Observables.combineLatest(fragmentLifecycle, loadMoreEvent)
      .filter { (lifecycle, _) -> lifecycle.isAtLeast(RxLifecycle.FragmentState.Resumed) }
      .observeOn(schedulerProvider.IO())
      .doOnNext {
        intercom.tell<GalleryFragment>()
          .to(GalleryFragmentEvent.GeneralEvents.PageIsLoading())
      }
      .flatMap { loadNextPageOfGalleryPhotos(viewState.lastId, viewState.photosPerPage) }
      .subscribe({ photos ->
        intercom.tell<GalleryFragment>()
          .to(GalleryFragmentEvent.GeneralEvents.ShowGalleryPhotos(photos))
      }, unknownErrors::onNext)
  }

  private fun loadNextPageOfGalleryPhotos(
    lastUploadedOn: Long,
    count: Int
  ): Observable<List<GalleryPhoto>> {
    return rxObservable(coroutineContext) {
      val photos = loadPageOfGalleryPhotos(lastUploadedOn, count)
      val result = getGalleryPhotosInfoUseCase.loadGalleryPhotosInfo(settingsRepository.getUserId(), photos)

      when (result) {
        is Either.Value -> {
          send(result.value)
        }
        is Either.Error -> {
          if (result.error is ApiException) {
            knownErrors.onNext(result.error.errorCode)
          } else {
            throw result.error
          }
        }
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