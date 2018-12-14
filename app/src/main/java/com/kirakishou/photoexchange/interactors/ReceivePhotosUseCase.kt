package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import core.ErrorCode
import kotlinx.coroutines.withContext
import net.response.data.ReceivedPhotoResponseData
import timber.log.Timber

open class ReceivePhotosUseCase(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val takenPhotosRepository: TakenPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "ReceivePhotosUseCase"

  suspend fun receivePhotos(
    photoData: FindPhotosData
  ): List<ReceivedPhoto> {
    return withContext(coroutineContext) {
      if (photoData.isUserIdEmpty()) {
        throw ReceivePhotosServiceException.UserIdIsEmptyException()
      }

      if (photoData.isPhotoNamesEmpty()) {
        throw ReceivePhotosServiceException.PhotoNamesAreEmpty()
      }

      return@withContext receivePhotos(photoData.userId!!, photoData.photoNames)
    }
  }

  private suspend fun receivePhotos(
    userId: String,
    photoNames: String
  ): List<ReceivedPhoto> {
    val receivedPhotos = try {
      apiClient.receivePhotos(userId, photoNames)
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

    return results
  }

  private suspend fun updatePhotoReceiverInfo(receivedPhoto: ReceivedPhotoResponseData) {
    database.transactional {
      if (!receivedPhotosRepository.save(receivedPhoto)) {
        throw DatabaseException("Could not save photo with receivedPhotoName ${receivedPhoto.receivedPhotoName}")
      }

      //we don't need to check the result here because it can be deleted at any time
      //and if it were deleted and updateReceiverInfo returns false we can ignore it
      uploadedPhotosRepository.updateReceiverInfo(
        receivedPhoto.uploadedPhotoName,
        receivedPhoto.receivedPhotoName,
        receivedPhoto.lon,
        receivedPhoto.lat
      )

      //TODO: is there any photo to delete to begin with? It should probably be deleted after uploading is done
      if (!takenPhotosRepository.deletePhotoByName(receivedPhoto.uploadedPhotoName)) {
        throw DatabaseException("Could not delete taken photo with uploadedPhotoName ${receivedPhoto.uploadedPhotoName}")
      }
    }
  }

  sealed class ReceivePhotosServiceException : Exception() {
    class PhotoNamesAreEmpty : ReceivePhotosServiceException()
    class UserIdIsEmptyException : ReceivePhotosServiceException()
    class ApiException(val errorCode: ErrorCode) : ReceivePhotosServiceException()
  }
}