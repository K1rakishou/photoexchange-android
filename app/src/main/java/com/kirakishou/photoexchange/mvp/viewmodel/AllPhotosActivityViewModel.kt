package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.extension.minutes
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewState
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.lang.ref.WeakReference

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
    private val LOCATION_CHECK_INTERVAL = 10.minutes()

    val onUploadingPhotoEventSubject = PublishSubject.create<PhotoUploadingEvent>().toSerialized()
    val myPhotosFragmentViewStateSubject = BehaviorSubject.createDefault<MyPhotosFragmentViewState>(MyPhotosFragmentViewState.Default()).toSerialized()

    override fun onAttached() {
        Timber.tag(tag).d("onAttached()")
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    fun startUploadingPhotosService(isGranted: Boolean): Maybe<Long> {
        return Single.fromCallable { photosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) }
            .subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
            .filter { count -> count > 0 }
            .doOnSuccess { myPhotosFragmentViewStateSubject.onNext(MyPhotosFragmentViewState.ShowObtainCurrentLocationNotification(true)) }
            .doOnSuccess { updateLastLocation(isGranted).blockingAwait() }
            .doOnSuccess { myPhotosFragmentViewStateSubject.onNext(MyPhotosFragmentViewState.ShowObtainCurrentLocationNotification(false)) }
    }

    fun loadUploadedPhotosFromDatabase(): Single<List<MyPhoto>> {
        return Single.fromCallable { photosRepository.findAllByState(PhotoState.PHOTO_UPLOADED) }
            .subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
    }

    private fun updateLastLocation(isGranted: Boolean): Completable {
        return Completable.fromAction {
            // if gps is disabled by user then set the last location as empty (-1.0, -1.0) immediately
            // so the user doesn't have to wait 15 seconds until getCurrentLocation returns empty
            // location because of timeout

            if (isGranted) {
                val now = TimeUtils.getTimeFast()
                val lastTimeCheck = settingsRepository.findLastLocationCheckTime()

                //request new location every 10 minutes
                if (lastTimeCheck == null || (now - lastTimeCheck > LOCATION_CHECK_INTERVAL)) {
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
}
