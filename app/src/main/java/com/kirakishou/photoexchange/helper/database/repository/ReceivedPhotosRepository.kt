package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.source.local.ReceivedPhotosLocalSource
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import net.response.ReceivedPhotosResponse

open class ReceivedPhotosRepository(
  private val database: MyDatabase,
  private val receivedPhotosLocalSource: ReceivedPhotosLocalSource
) : BaseRepository() {
  private val TAG = "ReceivedPhotosRepository"

  suspend fun save(receivedPhoto: ReceivedPhotosResponse.ReceivedPhotoResponseData): Boolean {
    return receivedPhotosLocalSource.save(receivedPhoto)
  }

  suspend fun save(photoExchangedData: PhotoExchangedData): Boolean {
    return receivedPhotosLocalSource.save(photoExchangedData)
  }

  suspend fun saveMany(receivedPhotos: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>): Boolean {
    return receivedPhotosLocalSource.saveMany(receivedPhotos)
  }

  suspend fun count(): Int {
    return receivedPhotosLocalSource.count()
  }

  suspend fun contains(uploadedPhotoName: String): Boolean {
    return receivedPhotosLocalSource.contains(uploadedPhotoName)
  }

  suspend fun getPage(lastUploadedOn: Long, count: Int): List<ReceivedPhoto> {
    return receivedPhotosLocalSource.getPage(lastUploadedOn, count)
  }

  suspend fun findMany(receivedPhotoIds: List<Long>): MutableList<ReceivedPhoto> {
    return receivedPhotosLocalSource.findMany(receivedPhotoIds)
  }

  suspend fun deleteByPhotoName(photoName: String) {
    receivedPhotosLocalSource.deleteByPhotoName(photoName)
  }

  suspend fun deleteAll() {
    receivedPhotosLocalSource.deleteAll()
  }

  suspend fun deleteOldPhotos() {
    receivedPhotosLocalSource.deleteOldPhotos()
  }

}