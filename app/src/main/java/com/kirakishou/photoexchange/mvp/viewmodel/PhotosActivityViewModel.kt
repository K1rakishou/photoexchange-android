package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.extension.drainErrorCodesTo
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.GetGalleryPhotosUseCase
import com.kirakishou.photoexchange.interactors.ReportPhotoUseCase
import com.kirakishou.photoexchange.mvp.model.*
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter
import com.kirakishou.photoexchange.ui.viewstate.PhotosActivityViewStateEvent
import com.kirakishou.photoexchange.ui.viewstate.UploadedPhotosFragmentViewStateEvent
import com.kirakishou.photoexchange.ui.viewstate.ReceivedPhotosFragmentViewStateEvent
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 3/11/2018.
 */
class PhotosActivityViewModel(
    private val takenPhotosRepository: TakenPhotosRepository,
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val receivedPhotosRepository: ReceivedPhotosRepository,
    private val getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
    private val favouritePhotoUseCase: FavouritePhotoUseCase,
    private val reportPhotoUseCase: ReportPhotoUseCase,
    private val schedulerProvider: SchedulerProvider
) : BaseViewModel<AllPhotosActivityView>() {

    private val TAG = "PhotosActivityViewModel"

    val onPhotoUploadEventSubject = PublishSubject.create<PhotoUploadEvent>().toSerialized()
    val onPhotoFindEventSubject = PublishSubject.create<ReceivePhotosEvent>().toSerialized()
    val photosActivityViewStateSubject = PublishSubject.create<PhotosActivityViewStateEvent>().toSerialized()
    val uploadedPhotosFragmentViewStateSubject = PublishSubject.create<UploadedPhotosFragmentViewStateEvent>().toSerialized()
    val receivedPhotosFragmentViewStateSubject = PublishSubject.create<ReceivedPhotosFragmentViewStateEvent>().toSerialized()
    val startPhotoUploadingServiceSubject = PublishSubject.create<Unit>().toSerialized()
    val startPhotoReceivingServiceSubject = PublishSubject.create<Unit>().toSerialized()
    val uploadedPhotosAdapterButtonClickSubject = PublishSubject.create<UploadedPhotosAdapter.UploadedPhotosAdapterButtonClickEvent>().toSerialized()
    val errorCodesSubject = PublishSubject.create<ErrorCode>().toSerialized()

    init {
        compositeDisposable += uploadedPhotosAdapterButtonClickSubject
            .flatMap {
                getView()?.handleUploadedPhotosFragmentAdapterButtonClicks(it) ?: Observable.just(false)
            }
            .filter { startUploadingService -> startUploadingService }
            .map { Unit }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe(startPhotoUploadingServiceSubject::onNext)
    }

    override fun onCleared() {
        Timber.tag(TAG).d("onCleared()")

        super.onCleared()
    }

    fun reportPhoto(photoName: String): Observable<Boolean> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .concatMap { userId -> reportPhotoUseCase.reportPhoto(userId, photoName) }
            .drainErrorCodesTo(errorCodesSubject)
            .subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun favouritePhoto(photoName: String): Observable<FavouritePhotoUseCase.FavouritePhotoResult> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .concatMap { userId -> favouritePhotoUseCase.favouritePhoto(userId, photoName) }
            .drainErrorCodesTo(errorCodesSubject)
            .subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun loadNextPageOfGalleryPhotos(lastId: Long, photosPerPage: Int): Observable<List<GalleryPhoto>> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .concatMap { userId ->
                getGalleryPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage)
                    .subscribeOn(schedulerProvider.IO())
                    .toObservable()
            }
            .drainErrorCodesTo(errorCodesSubject)
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun checkShouldStartReceivePhotosService() {
        compositeDisposable += Observable.fromCallable { takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) }
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            //do not start the service if there are queued up photos
            .filter { count -> count == 0 }
            .map {
                val uploadedPhotosCount = uploadedPhotosRepository.count()
                val receivedPhotosCount = receivedPhotosRepository.countAll()

                if (Constants.isDebugBuild) {
                    Timber.tag(TAG).d("uploadedPhotosCount: $uploadedPhotosCount, receivedPhotosCount: $receivedPhotosCount")
                }

                return@map uploadedPhotosCount > receivedPhotosCount
            }
            .filter { uploadedPhotosMoreThanReceived -> uploadedPhotosMoreThanReceived }
            .map { Unit }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe(startPhotoReceivingServiceSubject::onNext)
    }

    fun checkShouldStartPhotoUploadingService() {
        val observable = Observable.fromCallable { takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) }
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .publish()
            .autoConnect(2)

        compositeDisposable += observable
            .filter { count -> count == 0 }
            .doOnNext {
                if (Constants.isDebugBuild) {
                    Timber.tag(TAG).d("checkShouldStartPhotoUploadingService count == 0")
                }
            }
            .doOnNext { checkShouldStartReceivePhotosService() }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()

        compositeDisposable += observable
            .filter { count -> count > 0 }
            .doOnNext {
                if (Constants.isDebugBuild) {
                    Timber.tag(TAG).d("checkShouldStartPhotoUploadingService count > 0")
                }
            }
            .map { Unit }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe(startPhotoUploadingServiceSubject::onNext, startPhotoUploadingServiceSubject::onError)
    }

    fun loadReceivedPhotos(): Single<List<ReceivedPhoto>> {
        return Single.fromCallable { receivedPhotosRepository.findAll() }
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
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
            .observeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun forwardUploadPhotoEvent(event: PhotoUploadEvent) {
        onPhotoUploadEventSubject.onNext(event)
    }

    fun forwardPhotoFindEvent(event: ReceivePhotosEvent) {
        onPhotoFindEventSubject.onNext(event)
    }

    fun deletePhotoById(photoId: Long): Completable {
        return Completable.fromAction {
            takenPhotosRepository.deletePhotoById(photoId)
            if (Constants.isDebugBuild) {
                check(takenPhotosRepository.findById(photoId).isEmpty())
            }
        }.subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun changePhotoState(photoId: Long, newPhotoState: PhotoState): Completable {
        return Completable.fromAction { takenPhotosRepository.updatePhotoState(photoId, newPhotoState) }
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun updateGpsPermissionGranted(granted: Boolean): Completable {
        return Completable.fromAction {
            settingsRepository.updateGpsPermissionGranted(granted)
        }
    }
}
