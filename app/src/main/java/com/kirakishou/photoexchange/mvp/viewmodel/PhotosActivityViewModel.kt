package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.RxLifecycle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.entity.CachedPhotoIdEntity
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
    private val cachedPhotoIdRepository: CachedPhotoIdRepository,
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

    //Indicates whether the phone was rotated after fragment creation (savedStateInstance is NULL)
    val uploadedPhotosFragmentIsFreshlyCreated = BehaviorSubject.create<Boolean>().toSerialized()

    //Indicates what type of photos needs to be reloaded when swipeToRefresh getting triggered
    val uploadedPhotosFragmentPhotosTypeToRefresh = BehaviorSubject.create<PhotosToRefresh>().toSerialized()

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

    //Indicates whether the phone was rotated after fragment creation (savedStateInstance is NULL)
    val receivedPhotosFragmentIsFreshlyCreated = BehaviorSubject.create<Boolean>().toSerialized()

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

        //sets the type of photos that will be refreshed when user pulls SwipeToRefresh
        //
        //also disables endless scroller if photosToRefresh is QueuedUp or FailedToUpload
        //because we load them all at once
        compositeDisposable += uploadedPhotosFragmentPhotosTypeToRefresh
            .subscribeOn(schedulerProvider.IO())
            .subscribe({ photosToRefresh ->
                when (photosToRefresh!!) {
                    PhotosActivityViewModel.PhotosToRefresh.QueuedUp,
                    PhotosActivityViewModel.PhotosToRefresh.FailedToUpload -> {
                        intercom.tell<UploadedPhotosFragment>()
                            .to(UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling())
                    }
                    PhotosActivityViewModel.PhotosToRefresh.Uploaded -> {
                        intercom.tell<UploadedPhotosFragment>()
                            .to(UploadedPhotosFragmentEvent.GeneralEvents.EnableEndlessScrolling())
                    }
                }
            })

        //multicasts lifecycle to all listeners
        val onResumeObservable = uploadedPhotosFragmentLifecycle
            .subscribeOn(schedulerProvider.IO())
            .filter { lifecycle -> lifecycle.isAtLeast(RxLifecycle.FragmentState.Resumed) }
            .publish()

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
                    .concatMap { checkHasPhotosToUpload() }
                    .filter { hasQueuedUpPhotos -> hasQueuedUpPhotos }
                    .doOnNext { showProgress(isManualLoad) }
                    .doOnNext { uploadedPhotosFragmentPhotosTypeToRefresh.onNext(PhotosToRefresh.QueuedUp) }
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
                    .concatMap { checkHasPhotosToUpload() }
                    .filter { hasQueuedUpPhotos -> !hasQueuedUpPhotos }
                    .concatMap { checkHasFailedToUploadPhotos() }
                    .filter { hasFailedToUploadPhotos -> hasFailedToUploadPhotos }
                    .doOnNext { showProgress(isManualLoad) }
                    .doOnNext { uploadedPhotosFragmentPhotosTypeToRefresh.onNext(PhotosToRefresh.FailedToUpload) }
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
                    .concatMap { checkHasFailedToUploadPhotos() }
                    .filter { hasFailedToUploadPhotos -> !hasFailedToUploadPhotos }
                    .concatMap { checkHasPhotosToUpload() }
                    .filter { hasQueuedUpPhotos -> !hasQueuedUpPhotos }
                    .doOnNext { uploadedPhotosFragmentPhotosTypeToRefresh.onNext(PhotosToRefresh.Uploaded) }
                    .zipWith(uploadedPhotosFragmentIsFreshlyCreated)
                    .map { (_, isFragmentFreshlyCreated) -> isFragmentFreshlyCreated }
                    .concatMap { isFragmentFreshlyCreated ->
                        return@concatMap Observable.just(Unit)
                            .doOnNext { showProgress(isManualLoad) }
                            .concatMap {
                                return@concatMap loadNextPageOfUploadedPhotos(uploadedPhotosFragmentViewState.lastId,
                                    uploadedPhotosFragmentViewState.photosPerPage, isFragmentFreshlyCreated)
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

        compositeDisposable += onResumeObservable.connect()

        /**
         * When user pulls SwipeToRefresh control - multicast to everyone what type of photos we should refresh
         * */
        val photosToRefreshTypeObservable = uploadedPhotosFragmentRefreshPhotos
            .subscribeOn(schedulerProvider.IO())
            .zipWith(uploadedPhotosFragmentPhotosTypeToRefresh)
            .map { (_, photosType) -> photosType }
            .publish()

        //refresh failed to upload photos
        compositeDisposable += photosToRefreshTypeObservable
            .subscribeOn(schedulerProvider.IO())
            .filter { photosType -> photosType == PhotosToRefresh.FailedToUpload }
            .doOnNext { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.GeneralEvents.ClearAdapter()) }
            .map { true }
            .subscribe(uploadedPhotosFragmentLoadPhotosSubject::onNext, uploadedPhotosFragmentLoadPhotosSubject::onError)

        //refresh queued up photos
        compositeDisposable += photosToRefreshTypeObservable
            .subscribeOn(schedulerProvider.IO())
            .filter { photosType -> photosType == PhotosToRefresh.QueuedUp }
            .doOnNext { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.GeneralEvents.ClearAdapter()) }
            .map { true }
            .subscribe(uploadedPhotosFragmentLoadPhotosSubject::onNext, uploadedPhotosFragmentLoadPhotosSubject::onError)

        //start refreshing uploaded photos
        compositeDisposable += photosToRefreshTypeObservable
            .subscribeOn(schedulerProvider.IO())
            .filter { photosType -> photosType == PhotosToRefresh.Uploaded }
            .withLatestFrom(uploadedPhotosFragmentIsFreshlyCreated)
            .map { (_, isFragmentFreshlyCreated) -> isFragmentFreshlyCreated }
            .doOnNext { isFragmentFreshlyCreated -> clearPhotoIdsCacheMaybe(isFragmentFreshlyCreated, CachedPhotoIdEntity.PhotoType.UploadedPhoto) }
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
                    .withLatestFrom(receivedPhotosFragmentIsFreshlyCreated)
                    .map { (_, isFragmentFreshlyCreated) -> isFragmentFreshlyCreated }
                    .concatMap { isFragmentFreshlyCreated ->
                        return@concatMap Observable.just(Unit)
                            .doOnNext { showProgress(isManualLoad) }
                            .concatMap {
                                return@concatMap loadNextPageOfReceivedPhotos(receivedPhotosFragmentViewState.lastId,
                                    receivedPhotosFragmentViewState.photosPerPage, isFragmentFreshlyCreated)
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
            .withLatestFrom(receivedPhotosFragmentIsFreshlyCreated)
            .map { (_, isFragmentFreshlyCreated) -> isFragmentFreshlyCreated }
            .doOnNext { isFragmentFreshlyCreated -> clearPhotoIdsCacheMaybe(isFragmentFreshlyCreated, CachedPhotoIdEntity.PhotoType.ReceivedPhoto) }
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
        photosPerPage: Int,
        isFragmentFreshlyCreated: Boolean
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        return Observable.just(Unit)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { clearPhotoIdsCacheMaybe(isFragmentFreshlyCreated, CachedPhotoIdEntity.PhotoType.GalleryPhoto) }
            .concatMap { clearGalleryPhotoIdsCache() }
            .concatMap { loadPageOfGalleryPhotos(isFragmentFreshlyCreated, lastId, photosPerPage) }
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
        photosPerPage: Int,
        isFragmentFreshlyCreated: Boolean
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        return Observable.just(Unit)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { clearPhotoIdsCacheMaybe(isFragmentFreshlyCreated, CachedPhotoIdEntity.PhotoType.UploadedPhoto) }
            .concatMap { Observable.fromCallable { settingsRepository.getUserId() } }
            .concatMap { userId -> loadPageOfUploadedPhotos(userId, isFragmentFreshlyCreated, lastId, photosPerPage) }
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun loadNextPageOfReceivedPhotos(
        lastId: Long,
        photosPerPage: Int,
        isFragmentFreshlyCreated: Boolean
    ): Observable<Either<ErrorCode.GetReceivedPhotosErrors, MutableList<ReceivedPhoto>>> {
        return Observable.just(Unit)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { clearPhotoIdsCacheMaybe(isFragmentFreshlyCreated, CachedPhotoIdEntity.PhotoType.ReceivedPhoto) }
            .concatMap { Observable.fromCallable { settingsRepository.getUserId() } }
            .concatMap { userId -> loadPageOfReceivedPhotos(userId, isFragmentFreshlyCreated, lastId, photosPerPage) }
            .doOnNext { result ->
                if (result is Either.Value) {
                    intercom.tell<UploadedPhotosFragment>()
                        .to(UploadedPhotosFragmentEvent.GeneralEvents.UpdateReceiverInfo(result.value))
                }
            }
            .doOnError { Timber.tag(TAG).e(it) }
    }

    private fun loadPageOfGalleryPhotos(
        isFragmentFreshlyCreated: Boolean,
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        if (isFragmentFreshlyCreated || cachedPhotoIdRepository.isEmpty(CachedPhotoIdEntity.PhotoType.GalleryPhoto)) {
            return Observable.just(Unit)
                .doOnNext { intercom.tell<GalleryFragment>().to(GalleryFragmentEvent.GeneralEvents.ShowProgressFooter()) }
                .flatMap { getGalleryPhotosUseCase.loadPageOfPhotos(lastId, photosPerPage) }
                .doOnNext { result ->
                    if (result is Either.Value) {
                        val idsToCache = result.value.map { it.galleryPhotoId }
                        cachedPhotoIdRepository.insertMany(idsToCache, CachedPhotoIdEntity.PhotoType.GalleryPhoto)
                    }
                }
                .delay(adapterLoadMoreItemsDelayMs, TimeUnit.MILLISECONDS)
                .doOnEach { event ->
                    if (event.isOnNext || event.isOnError) {
                        intercom.tell<GalleryFragment>().to(GalleryFragmentEvent.GeneralEvents.HideProgressFooter())
                    }
                }
                .delay(progressFooterRemoveDelayMs, TimeUnit.MILLISECONDS)
        } else {
            return Observable.fromCallable {
                val cachedGalleryPhotoIds = cachedPhotoIdRepository.findAll(CachedPhotoIdEntity.PhotoType.GalleryPhoto)
                return@fromCallable Either.Value(galleryPhotoRepository.findMany(cachedGalleryPhotoIds))
            }
        }
    }

    private fun loadPageOfUploadedPhotos(
        userId: String,
        isFragmentFreshlyCreated: Boolean,
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        if (userId.isEmpty()) {
            return Observable.just<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>>(Either.Value(emptyList()))
        }

        if (isFragmentFreshlyCreated || cachedPhotoIdRepository.isEmpty(CachedPhotoIdEntity.PhotoType.UploadedPhoto)) {
            return Observable.just(Unit)
                .flatMap { getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage) }
                .doOnNext { result ->
                    if (result is Either.Value) {
                        val idsToCache = result.value.map { it.photoId }
                        cachedPhotoIdRepository.insertMany(idsToCache, CachedPhotoIdEntity.PhotoType.UploadedPhoto)
                    }
                }
        } else {
            return Observable.fromCallable {
                val cachedUploadedPhotoIds = cachedPhotoIdRepository.findAll(CachedPhotoIdEntity.PhotoType.UploadedPhoto)
                return@fromCallable Either.Value(uploadedPhotosRepository.findMany(cachedUploadedPhotoIds))
            }
        }
    }

    private fun loadPageOfReceivedPhotos(
        userId: String,
        isFragmentFreshlyCreated: Boolean,
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetReceivedPhotosErrors, MutableList<ReceivedPhoto>>> {
        if (userId.isEmpty()) {
            return Observable.just(Either.Error(ErrorCode.GetReceivedPhotosErrors.LocalUserIdIsEmpty()))
        }

        if (isFragmentFreshlyCreated || cachedPhotoIdRepository.isEmpty(CachedPhotoIdEntity.PhotoType.ReceivedPhoto)) {
            return Observable.just(Unit)
                .flatMap { getReceivedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage) }
                .doOnNext { result ->
                    if (result is Either.Value) {
                        val idsToCache = result.value.map { it.photoId }
                        cachedPhotoIdRepository.insertMany(idsToCache, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
                    }
                }
        } else {
            return Observable.fromCallable {
                val cachedReceivedPhotoIds = cachedPhotoIdRepository.findAll(CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
                return@fromCallable Either.Value(receivedPhotosRepository.findMany(cachedReceivedPhotoIds))
            }
        }
    }

    fun checkHasFailedToUploadPhotos(): Observable<Boolean> {
        return Observable.fromCallable {
            return@fromCallable takenPhotosRepository.countAllByState(PhotoState.FAILED_TO_UPLOAD) > 0
        }.subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
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

    private fun shouldClearGalleryPhotoIdsCache(): Boolean {
        val cachedGalleryPhotoIdsCount = cachedPhotoIdRepository.count(CachedPhotoIdEntity.PhotoType.GalleryPhoto)

        val uploadedPhotosCount = uploadedPhotosRepository.count()
        val receivedPhotosCount = receivedPhotosRepository.count()

        return (uploadedPhotosCount + receivedPhotosCount) > cachedGalleryPhotoIdsCount
    }

    private fun clearGalleryPhotoIdsCache(): Observable<Unit> {
        if (!shouldClearGalleryPhotoIdsCache()) {
            return Observable.just(Unit)
        }

        return Observable.fromCallable {
            cachedPhotoIdRepository.deleteAll(CachedPhotoIdEntity.PhotoType.GalleryPhoto)
        }
    }

    private fun clearPhotoIdsCacheMaybe(isFragmentFreshlyCreated: Boolean,
                                photoType: CachedPhotoIdEntity.PhotoType): Observable<Unit> {
        if (!isFragmentFreshlyCreated) {
            return Observable.just(Unit)
        }

        return clearPhotoIdsCache(photoType)
    }

    fun clearPhotoIdsCache(photoType: CachedPhotoIdEntity.PhotoType): Observable<Unit> {
        return Observable.fromCallable {
            cachedPhotoIdRepository.deleteAll(photoType)
        }.subscribeOn(schedulerProvider.IO())
    }

    enum class PhotosToRefresh {
        QueuedUp,
        FailedToUpload,
        Uploaded
    }
}
