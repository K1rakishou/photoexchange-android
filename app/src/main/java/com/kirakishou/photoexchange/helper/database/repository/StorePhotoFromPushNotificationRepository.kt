package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import kotlinx.coroutines.withContext
import timber.log.Timber

class StorePhotoFromPushNotificationRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "StorePhotoFromPushNotificationRepository"
  private val receivedPhotosDao = database.receivedPhotoDao()
  private val uploadedPhotoDao = database.uploadedPhotoDao()

  suspend fun storePhotoFromPushNotification(photoExchangedData: PhotoExchangedData): Boolean {
    return withContext(coroutineContext) {
      try {
        database.transactional {
          if (!save(photoExchangedData)) {
            throw DatabaseException("Could not save photoExchangedData as receivedPhoto, photoExchangedData = $photoExchangedData")
          }

          val result = updateReceiverInfo(
            photoExchangedData.uploadedPhotoName,
            photoExchangedData.receivedPhotoName,
            photoExchangedData.lon,
            photoExchangedData.lat
          )

          if (!result) {
            throw DatabaseException("Could not update receiverInfo for photo with uploadedPhotoName = ${photoExchangedData.uploadedPhotoName}")
          }
        }

        return@withContext true
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error)
        return@withContext false
      }
    }
  }

  private fun save(photoExchangedData: PhotoExchangedData): Boolean {
      val now = timeUtils.getTimeFast()

      val receivedPhotoEntity = ReceivedPhotoEntity(
        photoExchangedData.uploadedPhotoName,
        photoExchangedData.receivedPhotoName,
        photoExchangedData.lon,
        photoExchangedData.lat,
        photoExchangedData.uploadedOn,
        now
      )

      return receivedPhotosDao.save(receivedPhotoEntity).isSuccess()
  }

  private fun updateReceiverInfo(
    uploadedPhotoName: String,
    receivedPhotoName: String,
    lon: Double,
    lat: Double
  ): Boolean {
      return uploadedPhotoDao.updateReceiverInfo(uploadedPhotoName, receivedPhotoName, lon, lat) == 1
  }
}