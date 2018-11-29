package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.ReceivePhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.UploadPhotosLocalSource
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import kotlinx.coroutines.withContext
import net.response.ReceivedPhotosResponse
import timber.log.Timber

open class GetReceivedPhotosRepository(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val receivedPhotosLocalSource: ReceivePhotosLocalSource,
  private val uploadedPhotosLocalSource: UploadPhotosLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "GetReceivedPhotosRepository"

  open suspend fun getPage(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<ReceivedPhoto> {
    return withContext(coroutineContext) {
      receivedPhotosLocalSource.deleteOldPhotos()

      val receivedPhotos = getPageInternal(userId, lastUploadedOn, count)
      return@withContext receivedPhotos
        .sortedByDescending { it.uploadedOn }
    }
  }

  private suspend fun getPageInternal(userId: String, lastUploadedOn: Long, count: Int): List<ReceivedPhoto> {
    val pageOfReceivedPhotos = receivedPhotosLocalSource.getPageOfReceivedPhotos(lastUploadedOn, count)
    if (pageOfReceivedPhotos.size == count) {
      return pageOfReceivedPhotos
    }

    val receivedPhotos = apiClient.getReceivedPhotos(userId, lastUploadedOn, count)
    if (receivedPhotos.isEmpty()) {
      return pageOfReceivedPhotos
    }

    try {
      storeInDatabase(receivedPhotos)
    } catch (error: Throwable) {
      Timber.tag(TAG).e(error)
      throw error
    }

    return ReceivedPhotosMapper.FromResponse.GetReceivedPhotos.toReceivedPhotos(receivedPhotos)
  }

  private suspend fun storeInDatabase(receivedPhotos: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>) {
    database.transactional {
      for (receivedPhoto in receivedPhotos) {
        val updateResult = uploadedPhotosLocalSource.updateReceiverInfo(
          receivedPhoto.uploadedPhotoName,
          receivedPhoto.lon,
          receivedPhoto.lat
        )

        if (!updateResult) {
          //no uploaded photo in cached in the database by this name, skip it
          continue
        }
      }

      if (!receivedPhotosLocalSource.saveMany(receivedPhotos)) {
        throw DatabaseException("Could not cache received photos in the database")
      }
    }
  }
}