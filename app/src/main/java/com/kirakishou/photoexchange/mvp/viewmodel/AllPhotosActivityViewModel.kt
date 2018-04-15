package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.extension.seconds
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewStateEvent
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 3/11/2018.
 */
class AllPhotosActivityViewModel(
    view: WeakReference<AllPhotosActivityView>,
    private val photosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val schedulerProvider: SchedulerProvider
) : BaseViewModel<AllPhotosActivityView>(view) {

    private val tag = "[${this::class.java.simpleName}] "
    //TODO: changes LOCATION_CHECK_INTERVAL_MS from seconds to minutes
    private val LOCATION_CHECK_INTERVAL_MS = 2.seconds()
    private val SERVICE_START_DELAY_MS = 10.seconds()

    val onUploadingPhotoEventSubject = PublishSubject.create<PhotoUploadingEvent>().toSerialized()
    val myPhotosFragmentViewStateSubject = PublishSubject.create<MyPhotosFragmentViewStateEvent>().toSerialized()
    val startPhotoUploadingServiceSubject = PublishSubject.create<Unit>().toSerialized()

    override fun onAttached() {
        Timber.tag(tag).d("onAttached()")
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    private fun updateMyPhotosFragmentViewState(stateEvent: MyPhotosFragmentViewStateEvent) {
        myPhotosFragmentViewStateSubject.onNext(stateEvent)
    }

    fun checkShouldStartPhotoUploadingService(updateLastLocation: Boolean) {
        compositeDisposable += Observable.fromCallable { photosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) }
            .subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
            .filter { count -> count > 0 }
            .delay(1500, TimeUnit.MILLISECONDS)
            .debounce(SERVICE_START_DELAY_MS, TimeUnit.MILLISECONDS)
            .doOnNext { updateMyPhotosFragmentViewState(MyPhotosFragmentViewStateEvent.ShowObtainCurrentLocationNotification()) }
            .doOnNext { updateLastLocation(updateLastLocation).blockingAwait() }
            .doOnNext { updateMyPhotosFragmentViewState(MyPhotosFragmentViewStateEvent.HideObtainCurrentLocationNotification()) }
            .map { Unit }
            .subscribe(startPhotoUploadingServiceSubject::onNext, startPhotoUploadingServiceSubject::onError)
    }

    fun loadPhotos(): Single<MutableList<MyPhoto>> {
        return Single.fromCallable {
            val photos = mutableListOf<MyPhoto>()

            val queuedUpPhotos = photosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP)
            photos += queuedUpPhotos.sortedBy { it.id }

            val failedPhotos = photosRepository.findAllByState(PhotoState.FAILED_TO_UPLOAD)
            photos += failedPhotos.sortedBy { it.id }

            val uploadedPhotos = photosRepository.findAllByState(PhotoState.PHOTO_UPLOADED)
            photos += uploadedPhotos.sortedBy { it.id }

            return@fromCallable photos
        }.subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
    }

    private fun updateLastLocation(updateLastLocation: Boolean): Completable {
        return Completable.fromAction {
            // if gps is disabled by user then set the last location as empty (-1.0, -1.0) immediately
            // so the user doesn't have to wait 15 seconds until getCurrentLocation returns empty
            // location because of timeout

            if (updateLastLocation) {
                val now = TimeUtils.getTimeFast()
                val lastTimeCheck = settingsRepository.findLastLocationCheckTime()

                //request new location every 10 minutes
                if (lastTimeCheck == null || (now - lastTimeCheck > LOCATION_CHECK_INTERVAL_MS)) {
                    val currentLocation = getView()?.getCurrentLocation()?.blockingGet()
                        ?: return@fromAction

                    val lastLocation = settingsRepository.findLastLocation()
                    if (lastLocation != null && !lastLocation.isEmpty() && currentLocation.isEmpty()) {
                        return@fromAction
                    }

                    settingsRepository.saveLastLocationCheckTime(now)
                    settingsRepository.saveLastLocation(currentLocation)
                }
            } else {
                settingsRepository.saveLastLocation(LonLat.empty())
            }
        }
    }

    fun forwardUploadPhotoEvent(event: PhotoUploadingEvent) {
        onUploadingPhotoEventSubject.onNext(event)
    }

    fun deletePhotoById(photoId: Long): Completable {
        return Completable.fromAction {
            photosRepository.deletePhotoById(photoId)
            if (Constants.isDebugBuild) {
                check(photosRepository.findById(photoId).isEmpty())
            }
        }.subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
    }

    fun changePhotoState(photoId: Long, newPhotoState: PhotoState): Completable {
        return Completable.fromAction {
            photosRepository.updatePhotoState(photoId, newPhotoState)
        }.subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
    }
}
