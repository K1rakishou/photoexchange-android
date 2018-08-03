package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.RxLifecycle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUploadedPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import com.kirakishou.photoexchange.ui.viewstate.UploadedPhotosFragmentViewState
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class UploadedPhotosFragmentViewModel(
    private val takenPhotosRepository: TakenPhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
    private val schedulerProvider: SchedulerProvider,
    private val intercom: PhotosActivityViewModelIntercom,
    private val adapterLoadMoreItemsDelayMs: Long,
    private val progressFooterRemoveDelayMs: Long
) {
    private val TAG = "UploadedPhotosFragmentViewModel"

    val viewState = UploadedPhotosFragmentViewState()

    val fragmentLifecycle = BehaviorSubject.create<RxLifecycle.FragmentLifecycle>()
    val loadMoreEvent = PublishSubject.create<Unit>()
    val knownErrors = PublishSubject.create<ErrorCode>()
    val unknownErrors = PublishSubject.create<Throwable>()

    private val compositeDisposable = CompositeDisposable()

    init {
        val lifecycleObservable = fragmentLifecycle
            .filter { lifecycle -> lifecycle.isAtLeast(RxLifecycle.FragmentState.Resumed) }
            .publish()

        compositeDisposable += Observables.combineLatest(lifecycleObservable, loadMoreEvent)
            .observeOn(schedulerProvider.IO())
            .concatMap { figureOutWhatPhotosToLoad() }
            .filter { photosToLoad -> photosToLoad == PhotosToLoad.QueuedUpAndFailed }
            .doOnNext {
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling())
            }
            .concatMapSingle { tryToFixStalledPhotos() }
            .concatMapSingle { loadFailedToUploadPhotos().zipWith(loadQueuedUpPhotos()) }
            .subscribe({ (failedToUploadPhotos, queuedUpPhotos) ->
                if (queuedUpPhotos.isNotEmpty()) {
                    intercom.tell<PhotosActivity>()
                        .to(PhotosActivityEvent.StartUploadingService(PhotosActivityViewModel::class.java,
                            "There are queued up photos in the database"))
                }

                val combinedPhotos = combinePhotos(failedToUploadPhotos, queuedUpPhotos)

                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.GeneralEvents.ShowTakenPhotos(combinedPhotos))
            }, unknownErrors::onNext)

        compositeDisposable += Observables.combineLatest(lifecycleObservable, loadMoreEvent)
            .observeOn(schedulerProvider.IO())
            .concatMap { figureOutWhatPhotosToLoad() }
            .filter { photosToLoad -> photosToLoad == PhotosToLoad.Uploaded }
            .doOnNext {
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.GeneralEvents.EnableEndlessScrolling())
            }
            .doOnNext {
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.GeneralEvents.PageIsLoading())
            }
            .concatMap {
                loadNextPageOfUploadedPhotos(viewState.lastId, viewState.photosPerPage)
            }
            .subscribe({ result ->
                when (result) {
                    is Either.Value -> {
                        intercom.tell<UploadedPhotosFragment>()
                            .to(UploadedPhotosFragmentEvent.GeneralEvents.ShowUploadedPhotos(result.value))
                        intercom.tell<PhotosActivity>()
                            .to(PhotosActivityEvent.StartReceivingService(PhotosActivityViewModel::class.java,
                                "Starting the service after a page of uploaded photos was loaded"))
                    }
                    is Either.Error -> {
                        knownErrors.onNext(result.error)
                    }
                }
            }, unknownErrors::onNext)

        compositeDisposable += lifecycleObservable.connect()
    }

    private fun figureOutWhatPhotosToLoad(): Observable<PhotosToLoad> {
        return Observable.fromCallable {
            val hasFailedToUploadPhotos = takenPhotosRepository.countAllByState(PhotoState.FAILED_TO_UPLOAD) > 0
            val hasQueuedUpPhotos = takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0

            if (hasFailedToUploadPhotos || hasQueuedUpPhotos) {
                return@fromCallable PhotosToLoad.QueuedUpAndFailed
            }

            return@fromCallable PhotosToLoad.Uploaded
        }.subscribeOn(schedulerProvider.IO())
    }

    private fun combinePhotos(failedToUploadPhotos: List<TakenPhoto>, queuedUp: List<TakenPhoto>): MutableList<TakenPhoto> {
        val photos = mutableListOf<TakenPhoto>()

        photos += failedToUploadPhotos
        photos += queuedUp

        return photos
    }

    private fun tryToFixStalledPhotos(): Single<Unit> {
        return Single.fromCallable {
            takenPhotosRepository.tryToFixStalledPhotos()
        }
    }

    private fun loadFailedToUploadPhotos(): Single<List<TakenPhoto>> {
        return Single.fromCallable {
            return@fromCallable takenPhotosRepository.findAllByState(PhotoState.FAILED_TO_UPLOAD)
                .sortedBy { it.id }
        }.subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    private fun loadQueuedUpPhotos(): Single<List<TakenPhoto>> {
        return Single.fromCallable {
            return@fromCallable takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP)
                .sortedBy { it.id }
        }.subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    private fun loadNextPageOfUploadedPhotos(
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        return Observable.just(Unit)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { Observable.fromCallable { settingsRepository.getUserId() } }
            .concatMap { userId -> loadPageOfUploadedPhotos(userId, lastId, photosPerPage) }
            .doOnError { Timber.tag(TAG).e(it) }
    }

    private fun loadPageOfUploadedPhotos(
        userId: String,
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        if (userId.isEmpty()) {
            return Observable.just<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>>(Either.Value(emptyList()))
        }

        return Observable.just(Unit)
            .doOnNext { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter()) }
            .flatMap { getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage) }
            .delay(adapterLoadMoreItemsDelayMs, TimeUnit.MILLISECONDS)
            .doOnEach { event ->
                if (event.isOnNext || event.isOnError) {
                    intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.GeneralEvents.HideProgressFooter())
                }
            }
            .delay(progressFooterRemoveDelayMs, TimeUnit.MILLISECONDS)
    }

    fun onCleared() {
        Timber.tag(TAG).d("onCleared()")

        compositeDisposable.dispose()
    }

    enum class PhotosToLoad {
        QueuedUpAndFailed,
        Uploaded
    }
}