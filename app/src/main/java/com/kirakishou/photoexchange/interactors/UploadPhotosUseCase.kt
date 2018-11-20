package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.exception.ApiErrorException
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import core.ErrorCode
import kotlinx.coroutines.channels.SendChannel
import timber.log.Timber

open class UploadPhotosUseCase(
  private val database: MyDatabase,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val bitmapUtils: BitmapUtils,
  private val fileUtils: FileUtils
) {
  private val TAG = "UploadPhotosUseCase"

  suspend fun uploadPhoto(
    photo: TakenPhoto,
    location: LonLat,
    userId: String,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ) {
    if (!photo.fileExists()) {
      val path = photo.photoTempFile?.absolutePath ?: "(No photoTempFile)"
      Timber.tag(TAG).e("Photo does not exists on disk! $path")
      //TODO: add message
      throw PhotoUploadingException.PhotoDoesNotExistOnDisk()
    }

    val photoFile = fileUtils.createTempFile("rotated_photo", ".tmp")

    try {
      if (!takenPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADING)) {
        throw PhotoUploadingException.CouldNotUpdatePhotoState()
      }

      if (!bitmapUtils.rotatePhoto(photo.photoTempFile!!, photoFile)) {
        throw PhotoUploadingException.CouldNotRotatePhoto()
      }

      val result = try {
        apiClient.uploadPhoto(photoFile.absolutePath, location, userId, photo.isPublic, photo, channel)
      } catch (error: ApiErrorException) {
        throw PhotoUploadingException.ApiException(error.errorCode)
      }

      if (!movePhotoToUploaded(photo, result.photoId, result.photoName, location)) {
        throw PhotoUploadingException.DatabaseException()
      }

    } finally {
      fileUtils.deleteFile(photoFile)
    }
  }

  private suspend fun movePhotoToUploaded(
    photo: TakenPhoto,
    photoId: Long,
    photoName: String,
    location: LonLat
  ): Boolean {
    return database.transactional {
      val updateResult1 = takenPhotosRepository.deletePhotoById(photo.id)
      val updateResult2 = uploadedPhotosRepository.save(
        photoId,
        photoName,
        location.lon,
        location.lat,
        timeUtils.getTimeFast()
      )

      return@transactional updateResult1 && updateResult2
    }
  }

  data class UploadPhotoResult(
    val photoId: Long,
    val photoName: String
  )

  sealed class PhotoUploadingException : Exception() {
    class PhotoDoesNotExistOnDisk : PhotoUploadingException()
    class CouldNotRotatePhoto : PhotoUploadingException()
    class DatabaseException : PhotoUploadingException()
    class ApiException(val errorCode: ErrorCode) : PhotoUploadingException()
    class CouldNotUpdatePhotoState : PhotoUploadingException()
  }
}