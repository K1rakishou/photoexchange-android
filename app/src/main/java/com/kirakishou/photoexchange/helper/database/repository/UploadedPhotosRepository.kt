package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.source.local.UploadedPhotosLocalSource
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import net.response.data.UploadedPhotoResponseData

open class UploadedPhotosRepository(
  private val database: MyDatabase,
  private val uploadedPhotosLocalSource: UploadedPhotosLocalSource
) : BaseRepository() {
  private val TAG = "UploadedPhotosRepository"

  suspend fun save(photoId: Long, photoName: String, lon: Double, lat: Double, uploadedOn: Long): Boolean {
    return uploadedPhotosLocalSource.save(photoId, photoName, lon, lat, uploadedOn)
  }

  suspend fun saveMany(uploadedPhotoList: List<UploadedPhoto>): Boolean {
    return uploadedPhotosLocalSource.saveMany(uploadedPhotoList)
  }

  suspend fun count(): Int {
    return uploadedPhotosLocalSource.count()
  }

  suspend fun contains(photoName: String): Boolean {
    return uploadedPhotosLocalSource.contains(photoName)
  }

  suspend fun getPage(time: Long, count: Int): List<UploadedPhoto> {
    return uploadedPhotosLocalSource.getPage(time, count)
  }

  suspend fun findByPhotoName(uploadedPhotoName: String): UploadedPhoto? {
    return uploadedPhotosLocalSource.findByPhotoName(uploadedPhotoName)
  }

  suspend fun findMany(photoIds: List<Long>): List<UploadedPhoto> {
    return uploadedPhotosLocalSource.findMany(photoIds)
  }

  suspend fun findAll(): List<UploadedPhoto> {
    return uploadedPhotosLocalSource.findAll()
  }

  suspend fun findAllWithReceiverInfo(): List<UploadedPhoto> {
    return uploadedPhotosLocalSource.findAllWithReceiverInfo()
  }

  suspend fun findAllWithoutReceiverInfo(): List<UploadedPhoto> {
    return uploadedPhotosLocalSource.findAllWithoutReceiverInfo()
  }

  suspend fun updateReceiverInfo(
    uploadedPhotoName: String,
    receivedPhotoName: String,
    receiverLon: Double,
    receiverLat: Double
  ): Boolean {
    return uploadedPhotosLocalSource.updateReceiverInfo(
      uploadedPhotoName,
      receivedPhotoName,
      receiverLon,
      receiverLat
    )
  }

  suspend fun deleteAll() {
    uploadedPhotosLocalSource.deleteAll()
  }

  fun deleteByPhotoName(photoName: String) {
    uploadedPhotosLocalSource.deleteByPhotoName(photoName)
  }

}