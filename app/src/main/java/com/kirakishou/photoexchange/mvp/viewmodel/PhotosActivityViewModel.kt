package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.RxLifecycle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.model.*
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import com.kirakishou.photoexchange.ui.viewstate.ReceivedPhotosFragmentViewState
import com.kirakishou.photoexchange.ui.viewstate.UploadedPhotosFragmentViewState
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 3/11/2018.
 */
class PhotosActivityViewModel(
    private val takenPhotosRepository: TakenPhotosRepository,
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val galleryPhotoRepository: GalleryPhotoRepository,
    private val settingsRepository: SettingsRepository,
    private val receivedPhotosRepository: ReceivedPhotosRepository,
    private val getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
    private val getGalleryPhotosInfoUseCase: GetGalleryPhotosInfoUseCase,
    private val getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
    private val getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
    private val favouritePhotoUseCase: FavouritePhotoUseCase,
    private val reportPhotoUseCase: ReportPhotoUseCase,
    private val schedulerProvider: SchedulerProvider,
    private val adapterLoadMoreItemsDelayMs: Long,
    private val progressFooterRemoveDelayMs: Long
) : BaseViewModel() {

    private val TAG = "PhotosActivityViewModel"

    val intercom = PhotosActivityViewModelIntercom()

    /**
     * UploadedPhotosFragment
     * */
    val uploadedPhotosFragmentViewState = UploadedPhotosFragmentViewState()

    //Fragment lifecycle
    val uploadedPhotosFragmentLifecycle = BehaviorSubject.create<RxLifecycle.FragmentLifecycle>().toSerialized()

    //Indicates the refresh type:
    //- true means that this is a manual refresh (user pulled the SwipeToRefresh control),
    //  so we need to show swipeToRefresh progress
    //- false means that this is automatic refresh (usually getting triggered by endlessScroller),
    //  so we need to show progressbar
    val uploadedPhotosFragmentLoadPhotosSubject = PublishSubject.create<Boolean>().toSerialized()

    //Getting called when user pulls SwipeToRefresh control
    val uploadedPhotosFragmentRefreshPhotos = PublishSubject.create<Unit>().toSerialized()

    //All of the error codes go here
    val uploadedPhotosFragmentErrorCodeSubject = PublishSubject.create<ErrorCode.GetUploadedPhotosErrors>().toSerialized()

    /**
     * ReceivedPhotosFragment
     * */
    val receivedPhotosFragmentViewState = ReceivedPhotosFragmentViewState()

    //Fragment lifecycle
    val receivedPhotosFragmentLifecycle = BehaviorSubject.create<RxLifecycle.FragmentLifecycle>().toSerialized()

    //Indicates the refresh type:
    //- true means that this is a manual refresh (user pulled the SwipeToRefresh control),
    //  so we need to show swipeToRefresh progress
    //- false means that this is automatic refresh (usually getting triggered by endlessScroller),
    //  so we need to show progressbar
    val receivedPhotosFragmentLoadPhotosSubject = PublishSubject.create<Boolean>().toSerialized()

    //Getting called when user pulls SwipeToRefresh control
    val receivedPhotosFragmentRefreshPhotos = PublishSubject.create<Unit>().toSerialized()

    //All of the error codes go here
    val receivedPhotosFragmentErrorCodeSubject = PublishSubject.create<ErrorCode.GetReceivedPhotosErrors>().toSerialized()

    init {
        setupUploadedPhotosFragmentReactiveStreams()
        setupReceivedPhotosFragmentReactiveStreams()
    }

    override fun onCleared() {
        Timber.tag(TAG).d("onCleared()")

        super.onCleared()
    }

    private fun setupUploadedPhotosFragmentReactiveStreams() {
        //show SwipeToRefresh spinning progressBar if the isManualLoad is true,
        //or adds adapter progress footer otherwise
        fun showProgress(isManualLoad: Boolean) {
            if (isManualLoad) {
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.GeneralEvents.StartRefreshing())
            } else {
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter())
            }
        }

        //hides either SwipeToRefresh spinning progressBar or adapter progress footer
        fun hideProgress(isManualLoad: Boolean) {
            if (isManualLoad) {
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.GeneralEvents.StopRefreshing())
            } else {
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.GeneralEvents.HideProgressFooter())
            }
        }

        fun figureOutWhatPhotosToRefresh(): Observable<PhotosToRefresh> {
            return Observable.fromCallable {
                val hasPhotosToUpload = takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0
                if (hasPhotosToUpload) {
                    return@fromCallable PhotosToRefresh.QueuedUp
                }

                val hasFailedPhotos = takenPhotosRepository.countAllByState(PhotoState.FAILED_TO_UPLOAD) > 0
                if (hasFailedPhotos) {
                    return@fromCallable PhotosToRefresh.FailedToUpload
                }

                return@fromCallable PhotosToRefresh.Uploaded
            }
        }

        //multicasts lifecycle to all listeners
        val onResumeObservable = uploadedPhotosFragmentLifecycle
            .subscribeOn(schedulerProvider.IO())
            .filter { lifecycle -> lifecycle.isAtLeast(RxLifecycle.FragmentState.Resumed) }
            .publish()

//        val photosToRefreshObservable = figureOutWhatPhotosToRefresh()
//            .subscribeOn(schedulerProvider.IO())
//            .doOnNext { println("figureOutWhatPhotosToRefresh called, result = ${it}") }
//            .publish()

        /**
         * Photo loading in UploadedPhotosFragment has three conditions:
         * 1. If there are queued up photos - show them first
         * 2. If there are no queued up photos but there are failed to upload photos - show them next
         * 3. Otherwise show uploaded photos
         * */

        //load all queued up photos at once and show them in the recycler view
        compositeDisposable += uploadedPhotosFragmentLoadPhotosSubject.withLatestFrom(onResumeObservable)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { (isManualLoad, _) ->
                return@concatMap Observable.just(isManualLoad)
                    .zipWith(figureOutWhatPhotosToRefresh())
                    .map { (_, photosToRefresh) -> photosToRefresh }
                    .filter { photosToRefresh -> photosToRefresh == PhotosToRefresh.QueuedUp }
                    .doOnNext { showProgress(isManualLoad) }
                    //TODO: test queueUpAllFailedToUploadPhotos
                    .concatMapSingle { queueUpAllFailedToUploadPhotos() }
                    .concatMapSingle { loadQueuedUpPhotos() }
                    .doOnNext { hideProgress(isManualLoad) }
                    .doOnNext {
                        //TODO: test removed flag isManualLoad
                        intercom.tell<PhotosActivity>().to(PhotosActivityEvent.StartUploadingService(
                            PhotosActivityViewModel::class.java, "UploadingPhotosFragment Manual refresh"
                        ))
                    }
            }
            .subscribe({ queuedUpPhotos ->
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.GeneralEvents.AddQueuedUpPhotos(queuedUpPhotos))
            }, { error -> Timber.tag(TAG).e(error) })

        //load all failed to upload photos at once and show them in the recycler view
        compositeDisposable += uploadedPhotosFragmentLoadPhotosSubject.withLatestFrom(onResumeObservable)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { (isManualLoad, _) ->
                return@concatMap Observable.just(isManualLoad)
                    .zipWith(figureOutWhatPhotosToRefresh())
                    .map { (_, photosToRefresh) -> photosToRefresh }
                    .filter { photosToRefresh -> photosToRefresh == PhotosToRefresh.FailedToUpload }
                    .doOnNext { showProgress(isManualLoad) }
                    .concatMapSingle { loadFailedToUploadPhotos() }
                    .doOnNext { hideProgress(isManualLoad) }
            }
            .subscribe({ failedToUploadPhotos ->
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.GeneralEvents.AddFailedToUploadPhotos(failedToUploadPhotos))
            }, { error -> Timber.tag(TAG).e(error) })

        //start loading uploaded photos page by page
        compositeDisposable += uploadedPhotosFragmentLoadPhotosSubject.withLatestFrom(onResumeObservable)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { (isManualLoad, _) ->
                return@concatMap Observable.just(isManualLoad)
                    .zipWith(figureOutWhatPhotosToRefresh())
                    .filter { photosToRefresh -> photosToRefresh == PhotosToRefresh.Uploaded }
                    .concatMap { _ ->
                        return@concatMap Observable.just(Unit)
                            .doOnNext { showProgress(isManualLoad) }
                            .concatMap {
                                println("lastId = ${uploadedPhotosFragmentViewState.lastId}")
                                return@concatMap loadNextPageOfUploadedPhotos(uploadedPhotosFragmentViewState.lastId,
                                    uploadedPhotosFragmentViewState.photosPerPage)
                            }
                            .doOnNext { hideProgress(isManualLoad) }
                            .doOnNext {
                                if (isManualLoad) {
                                    intercom.tell<PhotosActivity>().to(PhotosActivityEvent.StartReceivingService(
                                        PhotosActivityViewModel::class.java, "UploadingPhotosFragment Manual refresh"
                                    ))
                                }
                            }
                    }
            }
            .subscribe({ result ->
                when (result) {
                    is Either.Error -> uploadedPhotosFragmentErrorCodeSubject.onNext(result.error)
                    is Either.Value -> {
                        intercom.tell<UploadedPhotosFragment>()
                            .to(UploadedPhotosFragmentEvent.GeneralEvents.AddUploadedPhotos(result.value))
                    }
                }
            }, { error -> Timber.tag(TAG).e(error) })

//        compositeDisposable += photosToRefreshObservable.connect()
        compositeDisposable += onResumeObservable.connect()

        /**
         * When user pulls SwipeToRefresh control - multicast to everyone what type of photos we should refresh
         * */
        val photosToRefreshTypeObservable = uploadedPhotosFragmentRefreshPhotos.zipWith(figureOutWhatPhotosToRefresh())
            .subscribeOn(schedulerProvider.IO())
            .map { (_, photosToRefresh) -> photosToRefresh }
            .publish()

        //refresh failed to upload photos
        compositeDisposable += photosToRefreshTypeObservable
            .subscribeOn(schedulerProvider.IO())
            .filter { photosToRefresh -> photosToRefresh == PhotosToRefresh.FailedToUpload }
            .doOnNext { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.GeneralEvents.ClearAdapter()) }
            .map { true }
            .subscribe(uploadedPhotosFragmentLoadPhotosSubject::onNext, uploadedPhotosFragmentLoadPhotosSubject::onError)

        //refresh queued up photos
        compositeDisposable += photosToRefreshTypeObservable
            .subscribeOn(schedulerProvider.IO())
            .filter { photosToRefresh -> photosToRefresh == PhotosToRefresh.QueuedUp }
            .doOnNext { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.GeneralEvents.ClearAdapter()) }
            .map { true }
            .subscribe(uploadedPhotosFragmentLoadPhotosSubject::onNext, uploadedPhotosFragmentLoadPhotosSubject::onError)

        //start refreshing uploaded photos
        compositeDisposable += photosToRefreshTypeObservable
            .subscribeOn(schedulerProvider.IO())
            .filter { photosToRefresh -> photosToRefresh == PhotosToRefresh.Uploaded }
            .doOnNext { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.GeneralEvents.ClearAdapter()) }
            .doOnNext { uploadedPhotosFragmentViewState.reset() }
            .map { true }
            .subscribe(uploadedPhotosFragmentLoadPhotosSubject::onNext, uploadedPhotosFragmentLoadPhotosSubject::onError)

        compositeDisposable += photosToRefreshTypeObservable.connect()
    }

    private fun setupReceivedPhotosFragmentReactiveStreams() {
        fun showProgress(isManualLoad: Boolean) {
            if (isManualLoad) {
                intercom.tell<ReceivedPhotosFragment>()
                    .to(ReceivedPhotosFragmentEvent.GeneralEvents.StartRefreshing())
            } else {
                intercom.tell<ReceivedPhotosFragment>()
                    .to(ReceivedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter())
            }
        }

        fun hideProgress(isManualLoad: Boolean) {
            if (isManualLoad) {
                intercom.tell<ReceivedPhotosFragment>()
                    .to(ReceivedPhotosFragmentEvent.GeneralEvents.StopRefreshing())
            } else {
                intercom.tell<ReceivedPhotosFragment>()
                    .to(ReceivedPhotosFragmentEvent.GeneralEvents.HideProgressFooter())
            }
        }

        val onResumeObservable = receivedPhotosFragmentLifecycle
            .subscribeOn(schedulerProvider.IO())
            .filter { lifecycle -> lifecycle.isAtLeast(RxLifecycle.FragmentState.Resumed) }
            .publish()

        compositeDisposable += Observables.combineLatest(receivedPhotosFragmentLoadPhotosSubject, onResumeObservable)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { (isManualLoad, _) ->
                return@concatMap Observable.just(isManualLoad)
                    .concatMap { _ ->
                        return@concatMap Observable.just(Unit)
                            .doOnNext { showProgress(isManualLoad) }
                            .concatMap {
                                return@concatMap loadNextPageOfReceivedPhotos(receivedPhotosFragmentViewState.lastId,
                                    receivedPhotosFragmentViewState.photosPerPage)
                            }
                            .doOnNext { hideProgress(isManualLoad) }
                    }
            }
            .subscribe({ result ->
                when (result) {
                    is Either.Error -> receivedPhotosFragmentErrorCodeSubject.onNext(result.error)
                    is Either.Value -> {
                        intercom.tell<ReceivedPhotosFragment>()
                            .to(ReceivedPhotosFragmentEvent.GeneralEvents.AddReceivedPhotos(result.value))
                    }
                }
            }, { error -> Timber.tag(TAG).e(error) })

        compositeDisposable += onResumeObservable.connect()

        compositeDisposable += receivedPhotosFragmentRefreshPhotos
            .subscribeOn(schedulerProvider.IO())
            .doOnNext { intercom.tell<ReceivedPhotosFragment>().to(ReceivedPhotosFragmentEvent.GeneralEvents.ClearAdapter()) }
            .doOnNext { receivedPhotosFragmentViewState.reset() }
            .doOnNext { receivedPhotosFragmentLoadPhotosSubject.onNext(true) }
            .subscribe()
    }

    fun reportPhoto(photoName: String): Observable<Either<ErrorCode.ReportPhotoErrors, Boolean>> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .subscribeOn(schedulerProvider.IO())
            .concatMap { userId -> reportPhotoUseCase.reportPhoto(userId, photoName) }
    }

    fun favouritePhoto(photoName: String): Observable<Either<ErrorCode.FavouritePhotoErrors, FavouritePhotoUseCase.FavouritePhotoResult>> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .subscribeOn(schedulerProvider.IO())
            .concatMap { userId -> favouritePhotoUseCase.favouritePhoto(userId, photoName) }
    }

    fun loadNextPageOfGalleryPhotos(
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        return Observable.just(Unit)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { loadPageOfGalleryPhotos(lastId, photosPerPage) }
            .concatMap { result ->
                if (result !is Either.Value) {
                    return@concatMap Observable.just(result)
                }

                //TODO: probably should add a check here to retrieve galleryPhotosInfo from the
                //server only when userId is not empty (user has uploaded at least one photo)
                val userId = settingsRepository.getUserId()
                return@concatMap getGalleryPhotosInfoUseCase.loadGalleryPhotosInfo(userId, result.value)
            }
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun loadNextPageOfUploadedPhotos(
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        return Observable.just(Unit)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { Observable.fromCallable { settingsRepository.getUserId() } }
            .concatMap { userId -> loadPageOfUploadedPhotos(userId, lastId, photosPerPage) }
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun loadNextPageOfReceivedPhotos(
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
            .doOnError { Timber.tag(TAG).e(it) }
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

    private fun loadPageOfUploadedPhotos(
        userId: String,
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        if (userId.isEmpty()) {
            return Observable.just<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>>(Either.Value(emptyList()))
        }

        return getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage)
    }

    private fun loadPageOfReceivedPhotos(
        userId: String,
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetReceivedPhotosErrors, MutableList<ReceivedPhoto>>> {
        if (userId.isEmpty()) {
            return Observable.just(Either.Error(ErrorCode.GetReceivedPhotosErrors.LocalUserIdIsEmpty()))
        }

        return getReceivedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage)
    }

    fun checkHasPhotosToUpload(): Observable<Boolean> {
        return Observable.fromCallable {
            return@fromCallable takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0
        }.subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun checkHasPhotosToReceive(): Observable<Boolean> {
        return Observable.fromCallable {
            val uploadedPhotosCount = uploadedPhotosRepository.count()
            val receivedPhotosCount = receivedPhotosRepository.count()

            return@fromCallable uploadedPhotosCount > receivedPhotosCount
        }.subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun loadFailedToUploadPhotos(): Single<List<TakenPhoto>> {
        return Single.fromCallable {
            takenPhotosRepository.tryToFixStalledPhotos()
            return@fromCallable takenPhotosRepository.findAllByState(PhotoState.FAILED_TO_UPLOAD)
                .sortedBy { it.id }
        }.subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun queueUpAllFailedToUploadPhotos(): Single<Unit> {
        return Single.fromCallable {
            return@fromCallable takenPhotosRepository.updateStates(PhotoState.FAILED_TO_UPLOAD, PhotoState.PHOTO_QUEUED_UP)
        }
    }

    fun loadQueuedUpPhotos(): Single<List<TakenPhoto>> {
        return Single.fromCallable {
            return@fromCallable takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP)
                .sortedBy { it.id }
        }.subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun deletePhotoById(photoId: Long): Completable {
        return Completable.fromAction {
            takenPhotosRepository.deletePhotoById(photoId)
            if (Constants.isDebugBuild) {
                check(takenPhotosRepository.findById(photoId).isEmpty())
            }
        }.subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun changePhotoState(photoId: Long, newPhotoState: PhotoState): Completable {
        return Completable.fromAction { takenPhotosRepository.updatePhotoState(photoId, newPhotoState) }
            .subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun updateGpsPermissionGranted(granted: Boolean): Completable {
        return Completable.fromAction {
            settingsRepository.updateGpsPermissionGranted(granted)
        }
    }

    enum class PhotosToRefresh {
        QueuedUp,
        FailedToUpload,
        Uploaded
    }
}
