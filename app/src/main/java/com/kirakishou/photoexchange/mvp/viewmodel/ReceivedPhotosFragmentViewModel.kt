package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.RxLifecycle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetReceivedPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import com.kirakishou.photoexchange.ui.viewstate.ReceivedPhotosFragmentViewState
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ReceivedPhotosFragmentViewModel(
    private val settingsRepository: SettingsRepository,
    private val getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
    private val schedulerProvider: SchedulerProvider,
    private val intercom: PhotosActivityViewModelIntercom,
    private val adapterLoadMoreItemsDelayMs: Long,
    private val progressFooterRemoveDelayMs: Long
) {
    private val TAG = "ReceivedPhotosFragmentViewModel"

    val viewState = ReceivedPhotosFragmentViewState()

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
                intercom.tell<ReceivedPhotosFragment>()
                    .to(ReceivedPhotosFragmentEvent.GeneralEvents.PageIsLoading())
            }
            .concatMap { loadNextPageOfReceivedPhotos(viewState.lastId, viewState.photosPerPage) }
            .subscribe({ result ->
                when (result) {
                    is Either.Value -> {
                        intercom.tell<ReceivedPhotosFragment>()
                            .to(ReceivedPhotosFragmentEvent.GeneralEvents.ShowReceivedPhotos(result.value))
                    }
                    is Either.Error -> {
                        knownErrors.onNext(result.error)
                    }
                }
            }, unknownErrors::onNext)
    }

    private fun loadNextPageOfReceivedPhotos(
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetReceivedPhotosErrors, MutableList<ReceivedPhoto>>> {
        return Observable.just(Unit)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { Observable.fromCallable { settingsRepository.getUserId() } }
            .concatMap { userId -> loadPageOfReceivedPhotos(userId, lastId, photosPerPage) }
            .doOnNext { result ->
                if (result is Either.Value) {
                    intercom.tell<UploadedPhotosFragment>()
                        .to(UploadedPhotosFragmentEvent.GeneralEvents.UpdateReceiverInfo(result.value))
                }
            }
    }

    private fun loadPageOfReceivedPhotos(
        userId: String,
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetReceivedPhotosErrors, MutableList<ReceivedPhoto>>> {
        if (userId.isEmpty()) {
            return Observable.just(Either.Error(ErrorCode.GetReceivedPhotosErrors.LocalUserIdIsEmpty()))
        }

        return Observable.just(Unit)
            .flatMap { getReceivedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage) }
            .doOnNext { intercom.tell<ReceivedPhotosFragment>().to(ReceivedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter()) }
            .delay(adapterLoadMoreItemsDelayMs, TimeUnit.MILLISECONDS)
            .doOnEach { event ->
                if (event.isOnNext || event.isOnError) {
                    intercom.tell<ReceivedPhotosFragment>().to(ReceivedPhotosFragmentEvent.GeneralEvents.HideProgressFooter())
                }
            }
            .delay(progressFooterRemoveDelayMs, TimeUnit.MILLISECONDS)
    }

    fun onCleared() {
        Timber.tag(TAG).d("onCleared()")

        compositeDisposable.dispose()
    }
}