package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
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
                        .to(UploadedPhotosFragmentEvent.UiEvents.UpdateReceiverInfo(result.value))
                }
            }
            .doOnError { Timber.tag(TAG).e(it) }
    }

    private fun loadPageOfGalleryPhotos(
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        return Observable.just(Unit)
            .doOnNext { intercom.tell<GalleryFragment>().to(GalleryFragmentEvent.UiEvents.ShowProgressFooter()) }
            .flatMap { getGalleryPhotosUseCase.loadPageOfPhotos(lastId, photosPerPage) }
            .delay(adapterLoadMoreItemsDelayMs, TimeUnit.MILLISECONDS)
            .doOnEach { event ->
                if (event.isOnNext || event.isOnError) {
                    intercom.tell<GalleryFragment>().to(GalleryFragmentEvent.UiEvents.HideProgressFooter())
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

        return Observable.just(Unit)
            .doOnNext { intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.UiEvents.ShowProgressFooter()) }
            .flatMap { getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage) }
            .delay(adapterLoadMoreItemsDelayMs, TimeUnit.MILLISECONDS)
            .doOnEach { event ->
                if (event.isOnNext || event.isOnError) {
                    intercom.tell<UploadedPhotosFragment>().to(UploadedPhotosFragmentEvent.UiEvents.HideProgressFooter())
                }
            }
            .delay(progressFooterRemoveDelayMs, TimeUnit.MILLISECONDS)
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
            .doOnNext { intercom.tell<ReceivedPhotosFragment>().to(ReceivedPhotosFragmentEvent.UiEvents.ShowProgressFooter()) }
            .delay(adapterLoadMoreItemsDelayMs, TimeUnit.MILLISECONDS)
            .doOnEach { event ->
                if (event.isOnNext || event.isOnError) {
                    intercom.tell<ReceivedPhotosFragment>().to(ReceivedPhotosFragmentEvent.UiEvents.HideProgressFooter())
                }
            }
            .delay(progressFooterRemoveDelayMs, TimeUnit.MILLISECONDS)
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
}
