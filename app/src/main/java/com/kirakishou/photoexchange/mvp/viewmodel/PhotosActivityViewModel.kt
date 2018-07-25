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
import com.kirakishou.photoexchange.ui.viewstate.UploadedPhotosFragmentViewState
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
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

    val uploadedPhotosFragmentViewState = UploadedPhotosFragmentViewState()

    val uploadedPhotosFragmentIsFreshlyCreated = BehaviorSubject.create<Boolean>().toSerialized()
    val uploadedPhotosFragmentPhotosTypeToRefresh = BehaviorSubject.create<PhotosToRefresh>().toSerialized()
    val uploadedPhotosFragmentLifecycle = BehaviorSubject.create<RxLifecycle.FragmentLifecycle>().toSerialized()

    //true means that this is a manual refresh, so we need to show swipeToRefresh progress
    //false means that this is automatic refresh, so we need to show progressbar
    val uploadedPhotosFragmentLoadPhotosSubject = PublishSubject.create<Boolean>().toSerialized()
    val uploadedPhotosFragmentReloadPhotos = PublishSubject.create<Unit>().toSerialized()
    val uploadedPhotosFragmentErrorCodeSubject = PublishSubject.create<ErrorCode.GetUploadedPhotosErrors>()

    init {
        setupUploadedPhotosFragmentLifecycleListener()
    }

    override fun onCleared() {
        Timber.tag(TAG).d("onCleared()")

        super.onCleared()
    }

    private fun setupUploadedPhotosFragmentLifecycleListener() {
        fun showProgress(isManualLoad: Boolean) {
            if (isManualLoad) {
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.UiEvents.StartRefreshing())
            } else {
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.UiEvents.ShowProgressFooter())
            }
        }

        fun hideProgress(isManualLoad: Boolean) {
            if (isManualLoad) {
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.UiEvents.StopRefreshing())
            } else {
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.UiEvents.HideProgressFooter())
            }
        }

        compositeDisposable += uploadedPhotosFragmentPhotosTypeToRefresh
            .subscribeOn(schedulerProvider.IO())
            .subscribe({ photosToRefresh ->
                when (photosToRefresh) {
                    PhotosActivityViewModel.PhotosToRefresh.QueuedUp,
                    PhotosActivityViewModel.PhotosToRefresh.FailedToUpload -> {
                        intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.UiEvents.DisableEndlessScrolling())
                    }
                    PhotosActivityViewModel.PhotosToRefresh.Uploaded -> {
                        intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.UiEvents.EnableEndlessScrolling())
                    }
                }
            })

        val onResumeObservable = uploadedPhotosFragmentLifecycle
            .subscribeOn(schedulerProvider.IO())
            .filter { lifecycle -> lifecycle.isAtLeast(RxLifecycle.FragmentState.Resumed) }
            .publish()

        compositeDisposable += uploadedPhotosFragmentLoadPhotosSubject.withLatestFrom(onResumeObservable)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { (isManualLoad, _) ->
                return@concatMap Observable.just(isManualLoad)
                    .concatMap { checkHasFailedToUploadPhotos() }
                    .filter { hasFailedToUploadPhotos -> hasFailedToUploadPhotos }
                    .doOnNext { showProgress(isManualLoad) }
                    .doOnNext { uploadedPhotosFragmentPhotosTypeToRefresh.onNext(PhotosToRefresh.FailedToUpload) }
                    .concatMapSingle { loadFailedToUploadPhotos() }
                    .doOnNext { hideProgress(isManualLoad) }
            }
            .subscribe({ failedToUploadPhotos ->
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.UiEvents.AddFailedToUploadPhotos(failedToUploadPhotos))
            }, { error -> Timber.tag(TAG).e(error) })

        compositeDisposable += uploadedPhotosFragmentLoadPhotosSubject.withLatestFrom(onResumeObservable)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { (isManualLoad, _) ->
                return@concatMap Observable.just(isManualLoad)
                    .concatMap { checkHasFailedToUploadPhotos() }
                    .filter { hasFailedToUploadPhotos -> !hasFailedToUploadPhotos }
                    .concatMap { checkHasPhotosToUpload() }
                    .filter { hasQueuedUpPhotos -> hasQueuedUpPhotos }
                    .doOnNext { showProgress(isManualLoad) }
                    .doOnNext { uploadedPhotosFragmentPhotosTypeToRefresh.onNext(PhotosToRefresh.QueuedUp) }
                    .concatMapSingle { loadQueuedUpPhotos() }
                    .doOnNext { hideProgress(isManualLoad) }
                    .doOnNext {
                        intercom.tell<PhotosActivity>().to(PhotosActivityEvent.StartUploadingService(
                            PhotosActivityViewModel::class.java, "Manual refresh"
                        ))
                    }
            }
            .subscribe({ queuedUpPhotos ->
                intercom.tell<UploadedPhotosFragment>()
                    .to(UploadedPhotosFragmentEvent.UiEvents.AddQueuedUpPhotos(queuedUpPhotos))
            }, { error -> Timber.tag(TAG).e(error) })

        compositeDisposable += uploadedPhotosFragmentLoadPhotosSubject.withLatestFrom(onResumeObservable)
            .subscribeOn(schedulerProvider.IO())
            .concatMap { (isManualLoad, _) ->
                return@concatMap Observable.just(isManualLoad)
                    .concatMap { checkHasFailedToUploadPhotos() }
                    .filter { hasFailedToUploadPhotos -> !hasFailedToUploadPhotos }
                    .concatMap { checkHasPhotosToUpload() }
                    .filter { hasQueuedUpPhotos -> !hasQueuedUpPhotos }
                    .doOnNext { uploadedPhotosFragmentPhotosTypeToRefresh.onNext(PhotosToRefresh.Uploaded) }
                    .withLatestFrom(uploadedPhotosFragmentIsFreshlyCreated)
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
                                intercom.tell<PhotosActivity>().to(PhotosActivityEvent.StartReceivingService(
                                    PhotosActivityViewModel::class.java, "Manual refresh"
                                ))
                            }
                    }
            }
            .subscribe({ result ->
                when (result) {
                    is Either.Error -> uploadedPhotosFragmentErrorCodeSubject.onNext(result.error)
                    is Either.Value -> {
                        intercom.tell<UploadedPhotosFragment>()
                            .to(UploadedPhotosFragmentEvent.UiEvents.AddUploadedPhotos(result.value))
                    }
                }
            }, { error -> Timber.tag(TAG).e(error) })

        compositeDisposable += onResumeObservable.connect()

        val photosToRefreshTypeObservable = uploadedPhotosFragmentReloadPhotos
            .subscribeOn(schedulerProvider.IO())
            .zipWith(uploadedPhotosFragmentPhotosTypeToRefresh)
            .map { (_, photosType) -> photosType }
            .doOnNext { photosType -> Timber.tag(TAG).d("photos to refresh type changes, newType = $photosType") }
            .publish()

        compositeDisposable += photosToRefreshTypeObservable
            .subscribeOn(schedulerProvider.IO())
            .filter { photosType -> photosType == PhotosToRefresh.FailedToUpload }
            .doOnNext { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.UiEvents.ClearAdapter()) }
            .doOnNext { uploadedPhotosFragmentLoadPhotosSubject.onNext(true) }
            .subscribe()

        compositeDisposable += photosToRefreshTypeObservable
            .subscribeOn(schedulerProvider.IO())
            .filter { photosType -> photosType == PhotosToRefresh.QueuedUp }
            .doOnNext { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.UiEvents.ClearAdapter()) }
            .doOnNext { uploadedPhotosFragmentLoadPhotosSubject.onNext(true) }
            .subscribe()

        compositeDisposable += photosToRefreshTypeObservable
            .subscribeOn(schedulerProvider.IO())
            .filter { photosType -> photosType == PhotosToRefresh.Uploaded }
            .withLatestFrom(uploadedPhotosFragmentIsFreshlyCreated)
            .map { (_, isFragmentFreshlyCreated) -> isFragmentFreshlyCreated }
            .doOnNext { isFragmentFreshlyCreated -> clearPhotoIdsCache(isFragmentFreshlyCreated, CachedPhotoIdEntity.PhotoType.UploadedPhoto) }
            .doOnNext { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.UiEvents.ClearAdapter()) }
            .doOnNext { uploadedPhotosFragmentViewState.reset() }
            .doOnNext { uploadedPhotosFragmentLoadPhotosSubject.onNext(true) }
            .subscribe()

        compositeDisposable += photosToRefreshTypeObservable.connect()
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
            .concatMap { clearPhotoIdsCache(isFragmentFreshlyCreated, CachedPhotoIdEntity.PhotoType.GalleryPhoto) }
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
            .concatMap { clearPhotoIdsCache(isFragmentFreshlyCreated, CachedPhotoIdEntity.PhotoType.UploadedPhoto) }
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
            .concatMap { clearPhotoIdsCache(isFragmentFreshlyCreated, CachedPhotoIdEntity.PhotoType.ReceivedPhoto) }
            .concatMap { Observable.fromCallable { settingsRepository.getUserId() } }
            .concatMap { userId -> loadPageOfReceivedPhotos(userId, isFragmentFreshlyCreated, lastId, photosPerPage) }
            .doOnNext { result ->
                if (result is Either.Value) {
                    intercom.tell<UploadedPhotosFragment>()
                        .to(UploadedPhotosFragmentEvent.UiEvents.UpdateReceiverInfo(result.value))
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
                .doOnNext { intercom.tell<GalleryFragment>().to(GalleryFragmentEvent.UiEvents.ShowProgressFooter()) }
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
                        intercom.tell<GalleryFragment>().to(GalleryFragmentEvent.UiEvents.HideProgressFooter())
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
                .doOnNext { intercom.tell<ReceivedPhotosFragment>().to(ReceivedPhotosFragmentEvent.UiEvents.ShowProgressFooter()) }
                .doOnNext { result ->
                    if (result is Either.Value) {
                        val idsToCache = result.value.map { it.photoId }
                        cachedPhotoIdRepository.insertMany(idsToCache, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
                    }
                }
                .delay(adapterLoadMoreItemsDelayMs, TimeUnit.MILLISECONDS)
                .doOnEach { event ->
                    if (event.isOnNext || event.isOnError) {
                        intercom.tell<ReceivedPhotosFragment>().to(ReceivedPhotosFragmentEvent.UiEvents.HideProgressFooter())
                    }
                }
                .delay(progressFooterRemoveDelayMs, TimeUnit.MILLISECONDS)
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

    fun tryToFixStalledPhotos(): Completable {
        return Completable.fromAction {
            takenPhotosRepository.tryToFixStalledPhotos()
        }
    }

    fun loadFailedToUploadPhotos(): Single<List<TakenPhoto>> {
        return Single.fromCallable {
            return@fromCallable takenPhotosRepository.findAllByState(PhotoState.FAILED_TO_UPLOAD)
                .sortedBy { it.id }
        }.subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
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

    private fun clearPhotoIdsCache(isFragmentFreshlyCreated: Boolean,
                                   photoType: CachedPhotoIdEntity.PhotoType): Observable<Unit> {
        if (!isFragmentFreshlyCreated) {
            return Observable.just(Unit)
        }

        return Observable.fromCallable {
            cachedPhotoIdRepository.deleteAll(photoType)
        }
    }

    enum class PhotosToRefresh {
        QueuedUp,
        FailedToUpload,
        Uploaded
    }
}
