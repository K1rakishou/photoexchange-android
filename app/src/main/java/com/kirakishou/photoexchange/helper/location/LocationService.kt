package com.kirakishou.photoexchange.helper.location

import android.content.Context
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.extension.seconds
import com.kirakishou.photoexchange.helper.LonLat
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.rx2.awaitFirst
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

  suspend fun getCurrentLocation(): LonLat {
    if (!takenPhotosRepository.hasPhotosWithEmptyLocation()) {
      return LonLat.empty()
    }

    val gpsPermissionGranted = settingsRepository.isGpsPermissionGranted()

    return if (gpsPermissionGranted) {
      Timber.tag(TAG).d("Gps permission is granted")

      val currentLocation = getCurrentLocationInternal().awaitFirst()
      updateCurrentLocationForAllPhotosWithEmptyLocation(currentLocation)
      currentLocation
    } else {
      Timber.tag(TAG).d("Gps permission is not granted")
      LonLat.empty()
    }
  }

  private suspend fun updateCurrentLocationForAllPhotosWithEmptyLocation(location: LonLat) {
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