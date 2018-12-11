package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext
import timber.log.Timber

open class UploadPhotosUseCase(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val fileUtils: FileUtils,
  private val bitmapUtils: BitmapUtils,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "UploadPhotosUseCase"

  suspend fun uploadPhoto(
    photo: TakenPhoto,
    location: LonLat,
    userId: String,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ): UploadPhotosUseCase.UploadPhotoResult {
    return withContext(coroutineContext) {
      if (!photo.fileExists()) {
        val path = photo.photoTempFile?.absolutePath ?: "(No photoTempFile)"
        Timber.tag(TAG).e("Photo does not exists on disk! $path")
        //TODO: add message
        throw PhotoUploadingException.PhotoDoesNotExistOnDisk()
      }

      val photoFile = fileUtils.createTempFile("rotated_photo", ".tmp")

      try {
        if (!takenPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADING)) {
          throw UploadPhotosUseCase.PhotoUploadingException.CouldNotUpdatePhotoState()
        }

        if (!bitmapUtils.rotatePhoto(photo.photoTempFile!!, photoFile)) {
          throw UploadPhotosUseCase.PhotoUploadingException.CouldNotRotatePhoto()
        }

        val result = try {
          apiClient.uploadPhoto(photoFile.absolutePath, location, userId, photo.isPublic, photo, channel)
        } catch (error: ApiErrorException) {
          throw ApiErrorException(error.errorCode)
        }

        if (!updatePhotoAsUploaded(photo, result.photoId, result.photoName, location)) {
          throw DatabaseException("Could not update photo as uploaded")
        }

        return@withContext result
      } finally {
        fileUtils.deleteFile(photoFile)
      }
    }
  }

  private suspend fun updatePhotoAsUploaded(photo: TakenPhoto, photoId: Long, photoName: String, location: LonLat): Boolean {
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
    val photoName: String,
    val uploadedOn: Long
  )

  sealed class PhotoUploadingException : Exception() {
    class PhotoDoesNotExistOnDisk : PhotoUploadingException()
    class CouldNotRotatePhoto : PhotoUploadingException()
    class CouldNotUpdatePhotoState : PhotoUploadingException()
  }
}