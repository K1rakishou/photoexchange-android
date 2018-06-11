package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.extension.seconds
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelStateEventForwarder
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.model.*
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
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
    private val getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
    private val getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
    private val getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
    private val favouritePhotoUseCase: FavouritePhotoUseCase,
    private val reportPhotoUseCase: ReportPhotoUseCase,
    private val schedulerProvider: SchedulerProvider
) : BaseViewModel() {

    private val TAG = "PhotosActivityViewModel"
    private val ADAPTER_LOAD_MORE_ITEMS_DELAY_MS = 1.seconds()

    private val cachedUploadedPhotoIds = mutableListOf<Long>()
    private val cachedReceivedPhotoIds = mutableListOf<Long>()
    private val cachedGalleryPhotoIds = mutableListOf<Long>()

    val eventForwarder = PhotosActivityViewModelStateEventForwarder()

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
        return Observable.fromCallable { settingsRepository.getUserId() }
            .subscribeOn(schedulerProvider.IO())
            .concatMap { userId ->
                if (isFragmentFreshlyCreated) {
                    return@concatMap getGalleryPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage)
                        .toObservable()
                        .doOnNext { result ->
                            if (result is Either.Value) {
                                cachedGalleryPhotoIds += result.value.map { it.galleryPhotoId }
                            }
                        }
                        .delay(ADAPTER_LOAD_MORE_ITEMS_DELAY_MS, TimeUnit.MILLISECONDS)
                } else {
                    return@concatMap Observable.fromCallable {
                        return@fromCallable Either.Value(galleryPhotoRepository.findMany(cachedGalleryPhotoIds))
                    }
                }
            }
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun loadNextPageOfUploadedPhotos(
        lastId: Long,
        photosPerPage: Int,
        isFragmentFreshlyCreated: Boolean
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {

        return Observable.fromCallable { settingsRepository.getUserId() }
            .subscribeOn(schedulerProvider.IO())
            .flatMap { userId ->
                if (userId.isEmpty()) {
                    return@flatMap Observable.just<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>>(Either.Value(emptyList()))
                }

                if (isFragmentFreshlyCreated) {
                    return@flatMap getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage)
                        .doOnNext { result ->
                            if (result is Either.Value) {
                                cachedUploadedPhotoIds += result.value.map { it.photoId }
                            }
                        }
                        .delay(ADAPTER_LOAD_MORE_ITEMS_DELAY_MS, TimeUnit.MILLISECONDS)
                } else {
                    return@flatMap Observable.fromCallable {
                        return@fromCallable Either.Value(uploadedPhotosRepository.findMany(cachedUploadedPhotoIds))
                    }
                }
            }
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun loadNextPageOfReceivedPhotos(
        lastId: Long,
        photosPerPage: Int,
        isFragmentFreshlyCreated: Boolean
    ): Observable<Either<ErrorCode.GetReceivedPhotosErrors, MutableList<ReceivedPhoto>>> {

        return Observable.fromCallable { settingsRepository.getUserId() }
            .subscribeOn(schedulerProvider.IO())
            .flatMap { userId ->
                if (userId.isEmpty()) {
                    return@flatMap Observable.just(Either.Error(ErrorCode.GetReceivedPhotosErrors.LocalUserIdIsEmpty()))
                }

                if (isFragmentFreshlyCreated) {
                    return@flatMap getReceivedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage)
                        .doOnNext { result ->
                            if (result is Either.Value) {
                                cachedReceivedPhotoIds += result.value.map { it.photoId }
                            }
                        }
                        .delay(ADAPTER_LOAD_MORE_ITEMS_DELAY_MS, TimeUnit.MILLISECONDS)
                } else {
                    return@flatMap Observable.fromCallable {
                        return@fromCallable Either.Value(receivedPhotosRepository.findMany(cachedReceivedPhotoIds))
                    }
                }
            }
            .doOnNext { result ->
                if (result is Either.Value) {
                    updateUploadedPhotosReceiverInfo(result.value)
                }
            }
            .doOnError { Timber.tag(TAG).e(it) }
    }

    private fun updateUploadedPhotosReceiverInfo(receivedPhotos: MutableList<ReceivedPhoto>) {
        eventForwarder.sendUploadedPhotosFragmentEvent(UploadedPhotosFragmentEvent.UiEvents.UpdateReceiverInfo(receivedPhotos))
    }

    fun checkHasPhotosToUpload(): Observable<Boolean> {
        return Observable.fromCallable {
            takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0
        }.subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun checkHasPhotosToReceive(): Observable<Boolean> {
        return Observable.fromCallable {
            val uploadedPhotosCount = uploadedPhotosRepository.count()
            val receivedPhotosCount = receivedPhotosRepository.countAll()

            return@fromCallable uploadedPhotosCount > receivedPhotosCount
        }.subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun loadMyPhotos(): Single<MutableList<TakenPhoto>> {
        return Single.fromCallable {
            val photos = mutableListOf<TakenPhoto>()

            val uploadingPhotos = takenPhotosRepository.findAllByState(PhotoState.PHOTO_UPLOADING)
            photos += uploadingPhotos.sortedBy { it.id }

            val queuedUpPhotos = takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP)
            photos += queuedUpPhotos.sortedBy { it.id }

            val failedPhotos = takenPhotosRepository.findAllByState(PhotoState.FAILED_TO_UPLOAD)
            photos += failedPhotos.sortedBy { it.id }

            return@fromCallable photos
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
}
