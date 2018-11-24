package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.database.repository.UploadPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import core.ErrorCode
import kotlinx.coroutines.channels.SendChannel
import timber.log.Timber

open class UploadPhotosUseCase(
  private val uploadPhotosRepository: UploadPhotosRepository
) {
  private val TAG = "UploadPhotosUseCase"

  suspend fun uploadPhoto(
    photo: TakenPhoto,
    location: LonLat,
    userId: String,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ): UploadPhotosUseCase.UploadPhotoResult {
    if (!photo.fileExists()) {
      val path = photo.photoTempFile?.absolutePath ?: "(No photoTempFile)"
      Timber.tag(TAG).e("Photo does not exists on disk! $path")
      //TODO: add message
      throw PhotoUploadingException.PhotoDoesNotExistOnDisk()
    }

    return uploadPhotosRepository.uploadPhoto(photo, location, userId, channel)
  }

  data class UploadPhotoResult(
    val photoId: Long,
    val photoName: String,
    val uploadedOn: Long
  )

  sealed class PhotoUploadingException : Exception() {
    class PhotoDoesNotExistOnDisk : PhotoUploadingException()
    class CouldNotRotatePhoto : PhotoUploadingException()
    class DatabaseException : PhotoUploadingException()
    class ApiException(val errorCode: ErrorCode) : PhotoUploadingException()
    class CouldNotUpdatePhotoState : PhotoUploadingException()
  }
}