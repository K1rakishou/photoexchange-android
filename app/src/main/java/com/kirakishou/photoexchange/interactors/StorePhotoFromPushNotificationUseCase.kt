package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.mvp.model.NewReceivedPhoto
import kotlinx.coroutines.withContext
import timber.log.Timber

class StorePhotoFromPushNotificationUseCase(
  private val database: MyDatabase,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "StorePhotoFromPushNotificationUseCase"

  suspend fun storePhotoFromPushNotification(newReceivedPhoto: NewReceivedPhoto): Boolean {
    return withContext(coroutineContext) {
      try {
        database.transactional {
          if (!receivedPhotosRepository.save(newReceivedPhoto)) {
            throw DatabaseException("Could not save newReceivedPhoto as receivedPhoto, " +
              "newReceivedPhoto = $newReceivedPhoto")
          }

          val result = uploadedPhotosRepository.updateReceiverInfo(
            newReceivedPhoto.uploadedPhotoName,
            newReceivedPhoto.receivedPhotoName,
            newReceivedPhoto.lon,
            newReceivedPhoto.lat
          )

          if (!result) {
            throw DatabaseException("Could not update receiverInfo for photo with " +
              "uploadedPhotoName = ${newReceivedPhoto.uploadedPhotoName}")
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