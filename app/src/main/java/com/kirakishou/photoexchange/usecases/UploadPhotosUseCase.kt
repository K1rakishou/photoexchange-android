package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.ImageLoadingDisabledInSettings
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvrx.model.PhotoState
import com.kirakishou.photoexchange.mvrx.model.photo.TakenPhoto
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext

open class UploadPhotosUseCase(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val fileUtils: FileUtils,
  private val bitmapUtils: BitmapUtils,
  private val netUtils: NetUtils,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "UploadPhotosUseCase"

  suspend fun uploadPhoto(
    photo: TakenPhoto,
    userUuid: String,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ): UploadPhotosUseCase.UploadPhotoResult {
    return withContext(coroutineContext) {
      if (!netUtils.canLoadImages()) {
        throw ImageLoadingDisabledInSettings()
      }

      if (!photo.fileExists()) {
        val path = photo.photoTempFile?.absolutePath ?: "(No photoTempFile)"
        throw PhotoUploadingException.PhotoDoesNotExistOnDisk(
          "Photo does not exists on disk! ($path)"
        )
      }

      val photoFile = fileUtils.createTempFile("rotated_photo", ".tmp")

      try {
        if (!takenPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADING)) {
          throw UploadPhotosUseCase.PhotoUploadingException.CouldNotUpdatePhotoState(
            "Could not update photo state to PHOTO_UPLOADING for photo with id (${photo.id})"
          )
        }

        if (!bitmapUtils.rotatePhoto(photo.photoTempFile!!, photoFile)) {
          throw UploadPhotosUseCase.PhotoUploadingException.CouldNotRotatePhoto(
            "Could not rotate photo with path (${photo.photoTempFile!!.absolutePath} and photoFile (${photoFile.absolutePath}))"
          )
        }

        val result = apiClient.uploadPhoto(
          photoFile.absolutePath,
          userUuid,
          photo,
          channel
        )

        updatePhotoAsUploaded(
          photo,
          result.photoId,
          result.photoName,
          result.uploadedOn
        )

        return@withContext result
      } finally {
        fileUtils.deleteFile(photoFile)
      }
    }
  }

  private suspend fun updatePhotoAsUploaded(
    photo: TakenPhoto,
    photoId: Long,
    photoName: String,
    uploadedOn: Long
  ) {
    return database.transactional {
      //ignore result
      takenPhotosRepository.deletePhotoById(photo.id)

      if (!uploadedPhotosRepository.save(
          photoId,
          photoName,
          photo.location.lon,
          photo.location.lat,
          uploadedOn,
          timeUtils.getTimeFast())) {
        throw DatabaseException("Could not save new uploaded photo with id: ($photoId), name: ($photoName), uploadedOn: ($uploadedOn)")
      }
    }
  }

  data class UploadPhotoResult(
    val photoId: Long,
    val photoName: String,
    val uploadedOn: Long
  )

  sealed class PhotoUploadingException(msg: String) : Exception(msg) {
    class PhotoDoesNotExistOnDisk(msg: String) : PhotoUploadingException(msg)
    class CouldNotRotatePhoto(msg: String) : PhotoUploadingException(msg)
    class CouldNotUpdatePhotoState(msg: String) : PhotoUploadingException(msg)
  }
}