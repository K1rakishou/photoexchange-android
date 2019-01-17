package com.kirakishou.photoexchange.usecases

import android.content.Context
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.extension.seconds
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.location.MyLocationManager
import com.kirakishou.photoexchange.helper.location.RxLocationManager
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.rx2.awaitFirst
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

class GetCurrentLocationUseCase(
  private val context: Context,
  private val database: MyDatabase,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetCurrentLocationUseCase"
  private val GPS_LOCATION_OBTAINING_MAX_TIMEOUT_MS = 15.seconds()

  private val locationManager by lazy { MyLocationManager(context) }

  suspend fun getCurrentLocation(): LonLat {
    return withContext(coroutineContext) {
      if (!takenPhotosRepository.hasPhotosWithEmptyLocation()) {
        return@withContext LonLat.empty()
      }

      val gpsPermissionGranted = settingsRepository.isGpsPermissionGranted()

      return@withContext if (gpsPermissionGranted) {
        Timber.tag(TAG).d("Gps permission is granted")

        val currentLocation = getCurrentLocationInternal().awaitFirst()
        updateCurrentLocationForAllPhotosWithEmptyLocation(currentLocation)
        currentLocation
      } else {
        Timber.tag(TAG).d("Gps permission is not granted")
        LonLat.empty()
      }
    }
  }

  private suspend fun updateCurrentLocationForAllPhotosWithEmptyLocation(location: LonLat) {
    if (location.isEmpty()) {
      return
    }

    val allPhotosWithEmptyLocation = takenPhotosRepository.findAllWithEmptyLocation()
    if (allPhotosWithEmptyLocation.isEmpty()) {
      return
    }

    database.transactional {
      for (photo in allPhotosWithEmptyLocation) {
        if (!takenPhotosRepository.updatePhotoLocation(photo.id!!, location.lon, location.lat)) {
          return@transactional false
        }
      }

      return@transactional true
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