package com.kirakishou.photoexchange.helper.location

import android.content.Context
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.extension.seconds
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class LocationService(
    private val context: Context,
    private val takenPhotosRepository: TakenPhotosRepository,
    private val settingsRepository: SettingsRepository
) {
    private val TAG = "LocationService"
    private val GPS_LOCATION_OBTAINING_MAX_TIMEOUT_MS = 15.seconds()

    private val locationManager by lazy { MyLocationManager(context) }

    fun getCurrentLocation(): Observable<LonLat> {
        if (!takenPhotosRepository.hasPhotosWithEmptyLocation()) {
            return Observable.just(LonLat.empty())
        }

        val gpsPermissionGrantedObservable = Observable.fromCallable {
            settingsRepository.isGpsPermissionGranted()
        }.publish().autoConnect(2)

        val gpsGranted = gpsPermissionGrantedObservable
            .filter { permissionGranted -> permissionGranted }
            .doOnNext { Timber.tag(TAG).d("Gps permission is granted") }
            .flatMap { getCurrentLocationInternal() }
            .doOnNext { updateCurrentLocationForAllPhotosWithEmptyLocation(it) }

        val gpsNotGranted = gpsPermissionGrantedObservable
            .filter { permissionGranted -> !permissionGranted }
            .doOnNext { Timber.tag(TAG).d("Gps permission is not granted") }
            .map { LonLat.empty() }

        return Observable.merge(gpsGranted, gpsNotGranted)
    }

    private fun updateCurrentLocationForAllPhotosWithEmptyLocation(location: LonLat) {
        try {
            takenPhotosRepository.updateAllPhotosLocation(location)
        } catch (error: Throwable) {
            Timber.tag(TAG).e(error)
        }
    }

    private fun getCurrentLocationInternal(): Observable<LonLat> {
        return Observable.fromCallable { locationManager.isGpsEnabled() }
            .flatMap { isGpsEnabled ->
                if (!isGpsEnabled) {
                    return@flatMap Observable.just(LonLat.empty())
                }

                return@flatMap RxLocationManager.start(locationManager)
                    .observeOn(Schedulers.io())
                    .timeout(GPS_LOCATION_OBTAINING_MAX_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .onErrorReturnItem(LonLat.empty())
            }
    }
}