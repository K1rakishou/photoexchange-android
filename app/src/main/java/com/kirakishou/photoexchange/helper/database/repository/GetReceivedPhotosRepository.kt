package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.ReceivePhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.UploadPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.remote.GetReceivedPhotosRemoteSource
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.DatabaseException
import net.response.ReceivedPhotosResponse

open class GetReceivedPhotosRepository(
  private val database: MyDatabase,
  private val getReceivedPhotosRemoteSource: GetReceivedPhotosRemoteSource,
  private val receivedPhotosLocalSource: ReceivePhotosLocalSource,
  private val uploadedPhotosLocalSource: UploadPhotosLocalSource
) {

  open suspend fun getReceivedPhotos(userId: String, lastUploadedOn: Long, count: Int): List<ReceivedPhoto> {
    receivedPhotosLocalSource.deleteOldPhotos()

    val receivedPhotos = getReceivedPhotosInternal(userId, lastUploadedOn, count)
    return receivedPhotos
      .sortedByDescending { it.photoId }
  }

  private suspend fun getReceivedPhotosInternal(userId: String, lastUploadedOn: Long, count: Int): List<ReceivedPhoto> {
    val pageOfReceivedPhotos = receivedPhotosLocalSource.getPageOfReceivedPhotos(lastUploadedOn, count)
    if (pageOfReceivedPhotos.size == count) {
      return pageOfReceivedPhotos
    }

    val receivedPhotos = getReceivedPhotosRemoteSource.getReceivedPhotos(userId, lastUploadedOn, count)
    if (receivedPhotos.isEmpty()) {
      return pageOfReceivedPhotos
    }

    val transactionResult = storeInDatabase(receivedPhotos)
    if (!transactionResult) {
      throw DatabaseException("Could not cache received photos in the database")
    }

    return ReceivedPhotosMapper.FromResponse.GetReceivedPhotos.toReceivedPhotos(receivedPhotos)
  }

  private suspend fun storeInDatabase(receivedPhotos: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>): Boolean {
    return database.transactional {
      for (receivedPhoto in receivedPhotos) {
        if (!uploadedPhotosLocalSource.updateReceiverInfo(receivedPhoto.uploadedPhotoName)) {
          return@transactional false
        }
      }

      if (!receivedPhotosLocalSource.saveMany(receivedPhotos)) {
        return@transactional false
      }

      return@transactional true
    }
  }
}