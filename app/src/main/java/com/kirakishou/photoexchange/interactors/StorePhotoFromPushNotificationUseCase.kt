package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import kotlinx.coroutines.withContext
import timber.log.Timber

class StorePhotoFromPushNotificationUseCase(
  private val database: MyDatabase,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "StorePhotoFromPushNotificationUseCase"

  suspend fun storePhotoFromPushNotification(photoExchangedData: PhotoExchangedData): Boolean {
    return withContext(coroutineContext) {
      try {
        database.transactional {
          if (!receivedPhotosRepository.save(photoExchangedData)) {
            throw DatabaseException("Could not save photoExchangedData as receivedPhoto, " +
              "photoExchangedData = $photoExchangedData")
          }

          val result = uploadedPhotosRepository.updateReceiverInfo(
            photoExchangedData.uploadedPhotoName,
            photoExchangedData.receivedPhotoName,
            photoExchangedData.lon,
            photoExchangedData.lat
          )

          if (!result) {
            throw DatabaseException("Could not update receiverInfo for photo with " +
              "uploadedPhotoName = ${photoExchangedData.uploadedPhotoName}")
          }
        }

        return@withContext true
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error)
        return@withContext false
      }
    }
  }

}