package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.source.local.TakenPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.UploadPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.remote.UploadPhotosRemoteSource
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.exception.ApiErrorException
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext

class UploadPhotosRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val bitmapUtils: BitmapUtils,
  private val fileUtils: FileUtils,
  private val takenPhotosLocalSource: TakenPhotosLocalSource,
  private val uploadPhotosRemoteSource: UploadPhotosRemoteSource,
  private val uploadPhotosLocalSource: UploadPhotosLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {

  suspend fun uploadPhoto(
    photo: TakenPhoto,
    location: LonLat,
    userId: String,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ): UploadPhotosUseCase.UploadPhotoResult {
    return withContext(coroutineContext) {
      val photoFile = fileUtils.createTempFile("rotated_photo", ".tmp")

      try {
        if (!takenPhotosLocalSource.updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADING)) {
          throw UploadPhotosUseCase.PhotoUploadingException.CouldNotUpdatePhotoState()
        }

        if (!bitmapUtils.rotatePhoto(photo.photoTempFile!!, photoFile)) {
          throw UploadPhotosUseCase.PhotoUploadingException.CouldNotRotatePhoto()
        }

        val result = try {
          uploadPhotosRemoteSource.uploadPhoto(photoFile.absolutePath, location, userId, photo.isPublic, photo, channel)
        } catch (error: ApiErrorException) {
          throw UploadPhotosUseCase.PhotoUploadingException.ApiException(error.errorCode)
        }

        if (!updatePhotoAsUploaded(photo, result.photoId, result.photoName, location)) {
          throw UploadPhotosUseCase.PhotoUploadingException.DatabaseException()
        }

        return@withContext result
      } finally {
        fileUtils.deleteFile(photoFile)
      }
    }
  }

  private suspend fun updatePhotoAsUploaded(photo: TakenPhoto, photoId: Long, photoName: String, location: LonLat): Boolean {
    return database.transactional {
      val updateResult1 = takenPhotosLocalSource.deletePhotoById(photo.id)
      val updateResult2 = uploadPhotosLocalSource.save(
        photoId,
        photoName,
        location.lon,
        location.lat,
        timeUtils.getTimeFast()
      )

      return@transactional updateResult1 && updateResult2
    }
  }
}