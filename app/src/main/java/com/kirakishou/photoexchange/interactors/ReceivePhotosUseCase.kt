package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.ApiErrorException
import com.kirakishou.photoexchange.mvp.model.exception.DatabaseException
import core.ErrorCode
import net.response.ReceivePhotosResponse
import timber.log.Timber

open class ReceivePhotosUseCase(
  private val database: MyDatabase,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val apiClient: ApiClient
) {
  private val TAG = "ReceivePhotosUseCase"

  suspend fun receivePhotos(
    photoData: FindPhotosData
  ): List<Pair<ReceivedPhoto, String>> {
    if (photoData.isUserIdEmpty()) {
      throw ReceivePhotosServiceException.CouldNotGetUserId()
    }

    if (photoData.isPhotoNamesEmpty()) {
      throw ReceivePhotosServiceException.PhotoNamesAreEmpty()
    }

    val receivedPhotos = try {
      apiClient.receivePhotos(photoData.userId!!, photoData.photoNames)
    } catch (error: ApiErrorException) {
      throw ReceivePhotosServiceException.ApiException(error.errorCode)
    }

    return handleSuccessResult(receivedPhotos)
  }

  private suspend fun handleSuccessResult(
    receivedPhotos: List<ReceivePhotosResponse.ReceivedPhotoResponseData>
  ): List<Pair<ReceivedPhoto, String>> {
    val results = mutableListOf<Pair<ReceivedPhoto, String>>()

    for (receivedPhoto in receivedPhotos) {
      val transactionResult = tryToUpdatePhotoInTheDatabase(receivedPhoto)
      if (!transactionResult) {
        throw DatabaseException("Could not update uploaded photo's receiver info ")
      }

      val photoAnswer = ReceivedPhotosMapper.FromResponse
        .ReceivedPhotos.toReceivedPhoto(receivedPhoto)
      results += Pair(photoAnswer, photoAnswer.uploadedPhotoName)
    }

    return results
  }

  private suspend fun tryToUpdatePhotoInTheDatabase(
    receivedPhoto: ReceivePhotosResponse.ReceivedPhotoResponseData
  ): Boolean {
    return database.transactional {
      if (!receivedPhotosRepository.save(receivedPhoto)) {
        Timber.tag(TAG).w("Could not save photo with receivedPhotoName ${receivedPhoto.receivedPhotoName}")
        return@transactional false
      }

      if (!uploadedPhotosRepository.updateReceiverInfo(receivedPhoto.uploadedPhotoName)) {
        Timber.tag(TAG).w("Could not update receiver info with uploadedPhotoName ${receivedPhoto.uploadedPhotoName}")
        return@transactional false
      }

      //TODO: is there any photo to delete to begin with? It should probably be deleted after uploading is done
      if (!takenPhotosRepository.deletePhotoByName(receivedPhoto.uploadedPhotoName)) {
        Timber.tag(TAG).w("Could not delete taken photo with uploadedPhotoName ${receivedPhoto.uploadedPhotoName}")
        return@transactional false
      }

      return@transactional true
    }
  }

  sealed class ReceivePhotosServiceException : Exception() {
    class PhotoNamesAreEmpty : ReceivePhotosServiceException()
    class CouldNotGetUserId : ReceivePhotosServiceException()
    class ApiException(val errorCode: ErrorCode) : ReceivePhotosServiceException()
  }
}