package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.Paged
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

  /**
   * This method skips the database cache
   * */
  open suspend fun getFresh(userId: String, time: Long, count: Int): Paged<ReceivedPhoto> {
    return withContext(coroutineContext) {
      //get a page of fresh photos from the server
      val receivedPhotos = apiClient.getPageOfReceivedPhotos(userId, time, count)
      if (receivedPhotos.isEmpty()) {
        Timber.tag(TAG).d("No fresh received photos were found on the server")
        return@withContext Paged(emptyList<ReceivedPhoto>(), true)
      }

      try {
        storeInDatabase(receivedPhotos)
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error)
        throw error
      }

      val mappedPhotos = ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhotos(receivedPhotos)
      return@withContext Paged(mappedPhotos, mappedPhotos.size < count)
    }
  }

  /**
   * This method includes photos from the database cache
   * */
  open suspend fun getPage(userId: String, lastUploadedOn: Long, count: Int): Paged<ReceivedPhoto> {
    return withContext(coroutineContext) {
      val pageOfReceivedPhotos = receivedPhotosLocalSource.getPageOfReceivedPhotos(lastUploadedOn, count)
      if (pageOfReceivedPhotos.size == count) {
        Timber.tag(TAG).d("Found enough received photos in the database")
        return@withContext Paged(pageOfReceivedPhotos, false)
      }

      val receivedPhotos = apiClient.getPageOfReceivedPhotos(userId, lastUploadedOn, count)
      if (receivedPhotos.isEmpty()) {
        Timber.tag(TAG).d("No received photos were found on the server")
        return@withContext Paged(pageOfReceivedPhotos, true)
      }

      try {
        storeInDatabase(receivedPhotos)
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error)
        throw error
      }

      val mappedPhotos = ReceivedPhotosMapper.FromResponse.GetReceivedPhotos.toReceivedPhotos(receivedPhotos)
      return@withContext Paged(mappedPhotos, mappedPhotos.size < count)
    }
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