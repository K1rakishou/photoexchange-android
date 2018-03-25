package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import io.reactivex.Completable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
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
    private val coroutinesPool: CoroutineThreadPoolProvider
) : BaseViewModel<AllPhotosActivityView>(view) {

    private val tag = "[${this::class.java.simpleName}] "
    private val LOCATION_CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(5)

    val uploadedPhotosSubject = PublishSubject.create<List<MyPhoto>>()
    val startUploadingServiceSubject = PublishSubject.create<Unit>()

    override fun onAttached() {
        Timber.tag(tag).d("onAttached()")
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    fun startUploadingPhotosService(isGranted: Boolean) {
        compositeDisposable += Completable.fromAction {
            val count = photosRepository.countAllByState(PhotoState.PHOTO_TO_BE_UPLOADED)
            if (count > 0) {
                updateLastLocation(isGranted)
                startUploadingServiceSubject.onNext(Unit)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe()
    }

    fun loadUploadedPhotosFromDatabase() {
        compositeDisposable += Completable.fromAction {
            val uploadedPhotos = photosRepository.findAllByState(PhotoState.PHOTO_UPLOADED)
            if (uploadedPhotos.isNotEmpty()) {
                //TODO: change this to event
                uploadedPhotosSubject.onNext(uploadedPhotos)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .delay(500, TimeUnit.MILLISECONDS)
            .subscribe()
    }

    private fun updateLastLocation(isGranted: Boolean) {
        // if gps is disabled by user then set the last location as empty (-1.0, -1.0) immediately
        // so the user doesn't have to wait 15 seconds until getCurrentLocation returns empty
        // location because of timeout

        if (isGranted) {
            val now = TimeUtils.getTimeFast()
            val lastTimeCheck = settingsRepository.findLastLocationCheckTime()
            if (lastTimeCheck == null || (now - lastTimeCheck > LOCATION_CHECK_INTERVAL)) {
                val currentLocation = getView()?.getCurrentLocation()?.blockingGet()
                    ?: return

                val lastLocation = settingsRepository.findLastLocation()
                if (lastLocation != null && !lastLocation.isEmpty() && currentLocation.isEmpty()) {
                    return
                }

                settingsRepository.saveLastLocationCheckTime(now)
                settingsRepository.saveLastLocation(currentLocation)
            }
        } else {
            settingsRepository.saveLastLocation(LonLat.empty())
        }
    }
}
