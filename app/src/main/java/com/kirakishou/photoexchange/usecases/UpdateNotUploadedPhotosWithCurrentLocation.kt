package com.kirakishou.photoexchange.usecases

import android.content.Context
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.extension.seconds
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.location.MyLocationManager
import com.kirakishou.photoexchange.helper.location.RxLocationManager
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.rx2.awaitFirst
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

class UpdateNotUploadedPhotosWithCurrentLocation(
  private val context: Context,
  private val database: MyDatabase,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "UpdateNotUploadedPhotosWithCurrentLocation"
  private val GPS_LOCATION_OBTAINING_MAX_TIMEOUT_MS = 15.seconds()

  //TODO: probably should inject it into this usecase instead of injecting context
  private val locationManager by lazy { MyLocationManager(context) }

  suspend fun updatePhotos() {
    withContext(coroutineContext) {
      if (!takenPhotosRepository.hasPhotosWithEmptyLocation()) {
        Timber.tag(TAG).d("No photos with empty location")
        return@withContext
      }

      if (!settingsRepository.isGpsPermissionGranted()) {
        Timber.tag(TAG).d("Gps permission is not granted")
        return@withContext
      }

      Timber.tag(TAG).d("Gps permission is granted")

      try {
        val currentLocation = getCurrentLocationInternal().awaitFirst()
        updateCurrentLocationForAllPhotosWithEmptyLocation(currentLocation)
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error, "Could not get current location")
        return@withContext
      }
    }
  }

  private suspend fun updateCurrentLocationForAllPhotosWithEmptyLocation(location: LonLat) {
    if (location.isEmpty()) {
      Timber.tag(TAG).d("Cannot update photo's location because current location is empty")
      return
    }

    val allPhotosWithEmptyLocation = takenPhotosRepository.findAllWithEmptyLocation()
    if (allPhotosWithEmptyLocation.isEmpty()) {
      Timber.tag(TAG).d("Could not find any photos with empty location")
      return
    }

    database.transactional {
      for (photo in allPhotosWithEmptyLocation) {
        if (!takenPhotosRepository.updatePhotoLocation(photo.id!!, location.lon, location.lat)) {
          throw DatabaseException("Could not update location for photo with id (${photo.id}) with location (${location.lon}, ${location.lat})")
        }
      }
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