package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.ReceivePhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.TakenPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.UploadPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.remote.ReceivePhotosRemoteSource
import com.kirakishou.photoexchange.interactors.ReceivePhotosUseCase
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import kotlinx.coroutines.withContext
import net.response.ReceivedPhotosResponse
import timber.log.Timber

class ReceivePhotosRepository(
  private val database: MyDatabase,
  private val receivePhotosRemoteSource: ReceivePhotosRemoteSource,
  private val receivePhotosLocalSource: ReceivePhotosLocalSource,
  private val uploadedPhotosLocalSource: UploadPhotosLocalSource,
  private val takenPhotosLocalSource: TakenPhotosLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "ReceivePhotosRepository"

  suspend fun receivePhotos(userId: String, photoNames: String): List<ReceivedPhoto> {
    return withContext(coroutineContext) {
      val receivedPhotos = try {
        receivePhotosRemoteSource.receivePhotos(userId, photoNames)
      } catch (error: ApiErrorException) {
        throw ReceivePhotosUseCase.ReceivePhotosServiceException.ApiException(error.errorCode)
      }

      val results = mutableListOf<ReceivedPhoto>()

      for (receivedPhoto in receivedPhotos) {
        try {
          updatePhotoReceiverInfo(receivedPhoto)
        } catch (error: Throwable) {
          Timber.tag(TAG).e(error)
          throw error
        }

        val photoAnswer = ReceivedPhotosMapper.FromResponse
          .ReceivedPhotos.toReceivedPhoto(receivedPhoto)

        results += photoAnswer
      }

      return@withContext results
    }
  }

  private suspend fun updatePhotoReceiverInfo(
    receivedPhoto: ReceivedPhotosResponse.ReceivedPhotoResponseData
  ) {
    database.transactional {
      if (!receivePhotosLocalSource.save(receivedPhoto)) {
        throw DatabaseException("Could not save photo with receivedPhotoName ${receivedPhoto.receivedPhotoName}")
      }

      val updateResult = uploadedPhotosLocalSource.updateReceiverInfo(
        receivedPhoto.uploadedPhotoName,
        receivedPhoto.lon,
        receivedPhoto.lat
      )

      if (!updateResult) {
        throw DatabaseException("Could not update receiver info with uploadedPhotoName ${receivedPhoto.uploadedPhotoName}")
      }

      //TODO: is there any photo to delete to begin with? It should probably be deleted after uploading is done
      if (!takenPhotosLocalSource.deletePhotoByName(receivedPhoto.uploadedPhotoName)) {
        throw DatabaseException("Could not delete taken photo with uploadedPhotoName ${receivedPhoto.uploadedPhotoName}")
      }
    }
  }

}