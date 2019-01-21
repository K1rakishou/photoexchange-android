package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.source.local.ReceivedPhotosLocalSource
import com.kirakishou.photoexchange.mvrx.model.NewReceivedPhoto
import com.kirakishou.photoexchange.mvrx.model.photo.ReceivedPhoto
import net.response.data.ReceivedPhotoResponseData

open class ReceivedPhotosRepository(
  private val database: MyDatabase,
  private val receivedPhotosLocalSource: ReceivedPhotosLocalSource
) : BaseRepository() {
  private val TAG = "ReceivedPhotosRepository"

  suspend fun save(receivedPhoto: ReceivedPhoto): Boolean {
    return receivedPhotosLocalSource.saveMany(listOf(receivedPhoto))
  }

  suspend fun save(receivedPhoto: ReceivedPhotoResponseData): Boolean {
    return receivedPhotosLocalSource.save(receivedPhoto)
  }

  suspend fun save(newReceivedPhoto: NewReceivedPhoto): Boolean {
    return receivedPhotosLocalSource.save(newReceivedPhoto)
  }

  suspend fun saveMany(receivedPhotos: List<ReceivedPhoto>): Boolean {
    return receivedPhotosLocalSource.saveMany(receivedPhotos)
  }

  suspend fun count(): Int {
    return receivedPhotosLocalSource.count()
  }

  suspend fun contains(uploadedPhotoName: String): Boolean {
    return receivedPhotosLocalSource.contains(uploadedPhotoName)
  }

  suspend fun getPage(lastUploadedOn: Long?, count: Int): List<ReceivedPhoto> {
    return receivedPhotosLocalSource.getPage(lastUploadedOn, count)
  }

  suspend fun findMany(receivedPhotoIds: List<Long>): MutableList<ReceivedPhoto> {
    return receivedPhotosLocalSource.findMany(receivedPhotoIds)
  }

  suspend fun findOld(): List<ReceivedPhoto> {
    return receivedPhotosLocalSource.findOld()
  }

  suspend fun deleteByPhotoName(photoName: String) {
    receivedPhotosLocalSource.deleteByPhotoName(photoName)
  }

  suspend fun deleteAll() {
    receivedPhotosLocalSource.deleteAll()
  }
}