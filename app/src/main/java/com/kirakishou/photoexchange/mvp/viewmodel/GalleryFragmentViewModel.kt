package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.RxLifecycle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetGalleryPhotosInfoUseCase
import com.kirakishou.photoexchange.interactors.GetGalleryPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.viewstate.GalleryFragmentViewState
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class GalleryFragmentViewModel(
  private val imageLoader: ImageLoader,
  private val settingsRepository: SettingsRepository,
  private val getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
  private val getGalleryPhotosInfoUseCase: GetGalleryPhotosInfoUseCase,
  private val schedulerProvider: SchedulerProvider,
  private val adapterLoadMoreItemsDelayMs: Long,
  private val progressFooterRemoveDelayMs: Long
) {
  private val TAG = "GalleryFragmentViewModel"

  lateinit var intercom: PhotosActivityViewModelIntercom

  val viewState = GalleryFragmentViewState()

  val fragmentLifecycle = BehaviorSubject.create<RxLifecycle.FragmentLifecycle>()
  val loadMoreEvent = PublishSubject.create<Unit>()
  val knownErrors = PublishSubject.create<ErrorCode>()
  val unknownErrors = PublishSubject.create<Throwable>()

  private val compositeDisposable = CompositeDisposable()

  init {
    compositeDisposable += Observables.combineLatest(fragmentLifecycle, loadMoreEvent)
      .filter { (lifecycle, _) -> lifecycle.isAtLeast(RxLifecycle.FragmentState.Resumed) }
      .observeOn(schedulerProvider.IO())
      .doOnNext {
        intercom.tell<GalleryFragment>()
          .to(GalleryFragmentEvent.GeneralEvents.PageIsLoading())
      }
      .concatMap { loadNextPageOfGalleryPhotos(viewState.lastId, viewState.photosPerPage) }
      .subscribe({ result ->
        when (result) {
          is Either.Value -> {
            intercom.tell<GalleryFragment>()
              .to(GalleryFragmentEvent.GeneralEvents.ShowGalleryPhotos(result.value))
          }
          is Either.Error -> {
            knownErrors.onNext(result.error)
          }
        }
      }, unknownErrors::onNext)
  }

  private fun loadNextPageOfGalleryPhotos(
    lastId: Long,
    photosPerPage: Int
  ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
    return Observable.just(Unit)
      .subscribeOn(schedulerProvider.IO())
      .concatMap {
        return@concatMap loadPageOfGalleryPhotos(lastId, photosPerPage)
          .observeOn(schedulerProvider.UI())
          /**
           * preloadPhotos method uses glide internally and glide requires all it's
           * operations to be run on the main thread
           * */
          .flatMap(this::preloadPhotos)
      }
      .observeOn(schedulerProvider.IO())
      .concatMap { result ->
        if (result !is Either.Value) {
          return@concatMap Observable.just(result)
        }

        val userId = settingsRepository.getUserId()
        return@concatMap getGalleryPhotosInfoUseCase.loadGalleryPhotosInfo(userId, result.value)
      }
  }

  private fun loadPageOfGalleryPhotos(
    lastId: Long,
    photosPerPage: Int
  ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
    return Observable.just(Unit)
      .doOnNext { intercom.tell<GalleryFragment>().to(GalleryFragmentEvent.GeneralEvents.ShowProgressFooter()) }
      .flatMap { getGalleryPhotosUseCase.loadPageOfPhotos(lastId, photosPerPage) }
      .delay(adapterLoadMoreItemsDelayMs, TimeUnit.MILLISECONDS)
      .doOnEach { event ->
        if (event.isOnNext || event.isOnError) {
          intercom.tell<GalleryFragment>().to(GalleryFragmentEvent.GeneralEvents.HideProgressFooter())
        }
      }
      .delay(progressFooterRemoveDelayMs, TimeUnit.MILLISECONDS)
  }

  private fun preloadPhotos(
    result: Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>
  ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
    if (result is Either.Error) {
      return Observable.just(result)
    }

    return Observable.fromIterable((result as Either.Value).value)
      .flatMapSingle { galleryPhoto ->
        return@flatMapSingle imageLoader.preloadImageFromNetAsync(galleryPhoto.photoName)
          .doOnSuccess { result ->
            if (!result) {
              Timber.tag(TAG).w("Could not pre-load photo ${galleryPhoto.photoName}")
            }
          }
      }
      .toList()
      .toObservable()
      .map { result }
  }

  fun onCleared() {
    Timber.tag(TAG).d("onCleared()")

    compositeDisposable.dispose()
  }
}