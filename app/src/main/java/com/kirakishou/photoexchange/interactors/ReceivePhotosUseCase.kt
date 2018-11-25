package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.database.repository.ReceivePhotosRepository
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import core.ErrorCode

open class ReceivePhotosUseCase(
  private val receivePhotosRepository: ReceivePhotosRepository
) {
  private val TAG = "ReceivePhotosUseCase"

  suspend fun receivePhotos(
    photoData: FindPhotosData
  ): List<ReceivedPhoto> {
    if (photoData.isUserIdEmpty()) {
      throw ReceivePhotosServiceException.UserIdIsEmptyException()
    }

    if (photoData.isPhotoNamesEmpty()) {
      throw ReceivePhotosServiceException.PhotoNamesAreEmpty()
    }

    return receivePhotosRepository.receivePhotos(photoData.userId!!, photoData.photoNames)
  }

  sealed class ReceivePhotosServiceException : Exception() {
    class PhotoNamesAreEmpty : ReceivePhotosServiceException()
    class UserIdIsEmptyException : ReceivePhotosServiceException()
    class ApiException(val errorCode: ErrorCode) : ReceivePhotosServiceException()
  }
}