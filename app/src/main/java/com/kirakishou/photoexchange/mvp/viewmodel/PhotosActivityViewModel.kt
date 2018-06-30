package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.entity.CachedPhotoIdEntity
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.model.*
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
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
    private val schedulerProvider: SchedulerProvider
) : BaseViewModel() {

    private val TAG = "PhotosActivityViewModel"
    private val ADAPTER_LOAD_MORE_ITEMS_DELAY_MS = 800L
    private val PROGRESS_FOOTER_REMOVE_DELAY_MS = 200L

    val intercom = PhotosActivityViewModelIntercom()

    override fun onCleared() {
        Timber.tag(TAG).d("onCleared()")

        super.onCleared()
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
            .concatMap { clearPhotoIdsCache(isFragmentFreshlyCreated) }
            .concatMap { clearGalleryPhotoIdsCache() }
            .concatMap { loadPageOfGalleryPhotos(isFragmentFreshlyCreated, lastId, photosPerPage) }
            .concatMap { result ->
                if (result !is Either.Value) {
                    return@concatMap Observable.just(result)
                }

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
            .concatMap { clearPhotoIdsCache(isFragmentFreshlyCreated) }
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
            .concatMap { clearPhotoIdsCache(isFragmentFreshlyCreated) }
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
                .delay(ADAPTER_LOAD_MORE_ITEMS_DELAY_MS, TimeUnit.MILLISECONDS)
                .doOnEach { intercom.tell<GalleryFragment>().to(GalleryFragmentEvent.UiEvents.HideProgressFooter()) }
                .delay(PROGRESS_FOOTER_REMOVE_DELAY_MS, TimeUnit.MILLISECONDS)
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
                .doOnNext { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.UiEvents.ShowProgressFooter()) }
                .flatMap { getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage) }
                .doOnNext { result ->
                    if (result is Either.Value) {
                        val idsToCache = result.value.map { it.photoId }
                        cachedPhotoIdRepository.insertMany(idsToCache, CachedPhotoIdEntity.PhotoType.UploadedPhoto)
                    }
                }
                .delay(ADAPTER_LOAD_MORE_ITEMS_DELAY_MS, TimeUnit.MILLISECONDS)
                .doOnEach { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.UiEvents.HideProgressFooter()) }
                .delay(PROGRESS_FOOTER_REMOVE_DELAY_MS, TimeUnit.MILLISECONDS)
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
                .delay(ADAPTER_LOAD_MORE_ITEMS_DELAY_MS, TimeUnit.MILLISECONDS)
                .doOnEach { intercom.tell<ReceivedPhotosFragment>().to(ReceivedPhotosFragmentEvent.UiEvents.HideProgressFooter()) }
                .delay(PROGRESS_FOOTER_REMOVE_DELAY_MS, TimeUnit.MILLISECONDS)
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

    private fun clearPhotoIdsCache(isFragmentFreshlyCreated: Boolean): Observable<Unit> {
        if (!isFragmentFreshlyCreated) {
            return Observable.just(Unit)
        }

        return Observable.fromCallable {
            cachedPhotoIdRepository.deleteAll()
        }
    }
}
