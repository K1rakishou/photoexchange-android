package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.extension.minutes
import com.kirakishou.photoexchange.helper.extension.seconds
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.GetGalleryPhotosUseCase
import com.kirakishou.photoexchange.interactors.ReportPhotoUseCase
import com.kirakishou.photoexchange.mvp.model.*
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.viewstate.AllPhotosActivityViewStateEvent
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewStateEvent
import com.kirakishou.photoexchange.ui.viewstate.ReceivedPhotosFragmentViewStateEvent
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.asSingle
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 3/11/2018.
 */
class AllPhotosActivityViewModel(
    private val photosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val photoAnswerRepository: PhotoAnswerRepository,
    private val getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
    private val favouritePhotoUseCase: FavouritePhotoUseCase,
    private val reportPhotoUseCase: ReportPhotoUseCase,
    private val schedulerProvider: SchedulerProvider
) : BaseViewModel<AllPhotosActivityView>() {

    private val tag = "AllPhotosActivityViewModel"
    private val LOCATION_CHECK_INTERVAL_MS = 0.minutes()
    private val SERVICE_START_DEBOUNCE_TIME_MS = 10.seconds()
    private val CHECK_SHOULD_START_SERVICE_DELAY_MS = 1500L

    val onPhotoUploadEventSubject = PublishSubject.create<PhotoUploadEvent>().toSerialized()
    val onPhotoFindEventSubject = PublishSubject.create<PhotoFindEvent>().toSerialized()
    val allPhotosActivityViewStateSubject = PublishSubject.create<AllPhotosActivityViewStateEvent>().toSerialized()
    val myPhotosFragmentViewStateSubject = PublishSubject.create<MyPhotosFragmentViewStateEvent>().toSerialized()
    val receivedPhotosFragmentViewStateSubject = PublishSubject.create<ReceivedPhotosFragmentViewStateEvent>().toSerialized()
    val startPhotoUploadingServiceSubject = PublishSubject.create<Unit>().toSerialized()
    val startFindPhotoAnswerServiceSubject = PublishSubject.create<Unit>().toSerialized()
    val myPhotosAdapterButtonClickSubject = PublishSubject.create<MyPhotosAdapter.MyPhotosAdapterButtonClickEvent>().toSerialized()

    init {
        compositeDisposable += myPhotosAdapterButtonClickSubject
            .flatMap { getView()?.handleMyPhotoFragmentAdapterButtonClicks(it) ?: Observable.just(false) }
            .filter { startUploadingService -> startUploadingService }
            .map { Unit }
            .subscribe(startPhotoUploadingServiceSubject::onNext, startPhotoUploadingServiceSubject::onError)
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    fun reportPhoto(photoName: String): Observable<Boolean> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .concatMap { userId -> reportPhotoUseCase.reportPhoto(userId, photoName) }
            .subscribeOn(schedulerProvider.IO())
    }

    fun favouritePhoto(photoName: String): Observable<Pair<Boolean, Long>> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .concatMap { userId -> favouritePhotoUseCase.favouritePhoto(userId, photoName) }
            .subscribeOn(schedulerProvider.IO())
    }

    fun loadNextPageOfGalleryPhotos(lastId: Long, photosPerPage: Int): Observable<List<GalleryPhoto>> {
        return getGalleryPhotosUseCase.loadNextPageOfGalleryPhotos(lastId, photosPerPage)
            .subscribeOn(schedulerProvider.IO())
    }

    fun checkShouldStartFindPhotoAnswersService() {
        compositeDisposable += Observable.fromCallable { photosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) }
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            //do not start the service if there are queued up photos
            .filter { count -> count == 0 }
            .map {
                val uploadedPhotosCount = photosRepository.countAllByStates(arrayOf(PhotoState.PHOTO_UPLOADED, PhotoState.PHOTO_UPLOADED_ANSWER_RECEIVED))
                val receivedPhotosCount = photoAnswerRepository.countAll()

                if (Constants.isDebugBuild) {
                    Timber.tag(tag).e("uploadedPhotosCount: $uploadedPhotosCount, receivedPhotosCount: $receivedPhotosCount")
                }

                return@map uploadedPhotosCount > receivedPhotosCount
            }
            .filter { uploadedPhotosMoreThanReceived -> uploadedPhotosMoreThanReceived }
            .delay(CHECK_SHOULD_START_SERVICE_DELAY_MS, TimeUnit.MILLISECONDS)
            .map { Unit }
            .subscribe(startFindPhotoAnswerServiceSubject::onNext, startFindPhotoAnswerServiceSubject::onError)
    }

    fun checkShouldStartPhotoUploadingService(updateLastLocation: Boolean) {
        val observable = Observable.fromCallable { photosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) }
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .publish()
            .autoConnect(2)

        compositeDisposable += observable
            .filter { count -> count == 0 }
            .doOnNext { Timber.tag(tag).d("checkShouldStartPhotoUploadingService count == 0") }
            .doOnNext { checkShouldStartFindPhotoAnswersService() }
            .subscribe()

        compositeDisposable += observable
            .filter { count -> count > 0 }
            .delay(CHECK_SHOULD_START_SERVICE_DELAY_MS, TimeUnit.MILLISECONDS)
            .debounce(SERVICE_START_DEBOUNCE_TIME_MS, TimeUnit.MILLISECONDS)
            .doOnNext { Timber.tag(tag).d("checkShouldStartPhotoUploadingService count > 0") }
            .map { Unit }
            .concatMap {
                Observable.just(1)
                    .observeOn(schedulerProvider.IO())
                    .doOnNext { myPhotosFragmentViewStateSubject.onNext(MyPhotosFragmentViewStateEvent.ShowObtainCurrentLocationNotification()) }
                    .concatMap { updateLastLocation(updateLastLocation) }
                    .delay(1, TimeUnit.SECONDS)
                    .doOnNext { myPhotosFragmentViewStateSubject.onNext(MyPhotosFragmentViewStateEvent.HideObtainCurrentLocationNotification()) }
            }
            .doOnError { Timber.e(it) }
            .subscribe(startPhotoUploadingServiceSubject::onNext, startPhotoUploadingServiceSubject::onError)
    }

    private fun updateLastLocation(updateLastLocation: Boolean): Observable<Unit> {
        return async {
            // if gps is disabled by user then set the last location as empty (-1.0, -1.0) immediately
            // so the user doesn't have to wait 15 seconds until getCurrentLocation returns empty
            // location because of timeout

            if (updateLastLocation) {
                val now = TimeUtils.getTimeFast()
                val lastTimeCheck = settingsRepository.getLastLocationCheckTime()

                //request new location every LOCATION_CHECK_INTERVAL_MS
                if (lastTimeCheck == null || (now - lastTimeCheck > LOCATION_CHECK_INTERVAL_MS)) {
                    val currentLocation = try {
                        getView()?.getCurrentLocation()?.await()
                    } catch (error: Exception) {
                        LonLat.empty()
                    }

                    if (currentLocation == null) {
                        return@async
                    }

                    val lastLocation = settingsRepository.getLastLocation()
                    if (lastLocation != null && !lastLocation.isEmpty() && currentLocation.isEmpty()) {
                        return@async
                    }

                    settingsRepository.saveLastLocationCheckTime(now)
                    settingsRepository.saveLastLocation(currentLocation)
                }
            } else {
                settingsRepository.saveLastLocation(LonLat.empty())
            }

            return@async
        }.asSingle(CommonPool)
            .toObservable()
    }

    fun loadPhotoAnswers(): Single<List<PhotoAnswer>> {
        return Single.fromCallable { photoAnswerRepository.findAll() }
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
    }

    fun loadMyPhotos(): Single<MutableList<MyPhoto>> {
        return Single.fromCallable {
            val photos = mutableListOf<MyPhoto>()

            val uploadingPhotos = photosRepository.findAllByState(PhotoState.PHOTO_UPLOADING)
            photos += uploadingPhotos.sortedBy { it.id }

            val queuedUpPhotos = photosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP)
            photos += queuedUpPhotos.sortedBy { it.id }

            val failedPhotos = photosRepository.findAllByState(PhotoState.FAILED_TO_UPLOAD)
            photos += failedPhotos.sortedBy { it.id }

            val uploadedPhotos = photosRepository.findAllByState(PhotoState.PHOTO_UPLOADED)
            photos += uploadedPhotos.sortedBy { it.id }

            val uploadedAndReceivedAnswerPhotos = photosRepository.findAllByState(PhotoState.PHOTO_UPLOADED_ANSWER_RECEIVED)
            photos += uploadedAndReceivedAnswerPhotos.sortedBy { it.id }

            return@fromCallable photos
        }.subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
    }

    fun forwardUploadPhotoEvent(event: PhotoUploadEvent) {
        onPhotoUploadEventSubject.onNext(event)
    }

    fun forwardPhotoFindEvent(event: PhotoFindEvent) {
        onPhotoFindEventSubject.onNext(event)
    }

    fun deletePhotoById(photoId: Long): Completable {
        return Completable.fromAction {
            photosRepository.deletePhotoById(photoId)
            if (Constants.isDebugBuild) {
                check(photosRepository.findById(photoId).isEmpty())
            }
        }.subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
    }

    fun changePhotoState(photoId: Long, newPhotoState: PhotoState): Completable {
        return Completable.fromAction { photosRepository.updatePhotoState(photoId, newPhotoState) }
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
    }
}
