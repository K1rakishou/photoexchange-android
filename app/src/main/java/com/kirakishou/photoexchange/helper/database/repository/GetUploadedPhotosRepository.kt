package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.UploadPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.remote.GetUploadedPhotosRemoteSource
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.DatabaseException

class GetUploadedPhotosRepository(
  private val uploadedPhotosLocalSource: UploadPhotosLocalSource,
  private val getUploadedPhotosRemoteSource: GetUploadedPhotosRemoteSource
) {

  suspend fun getPage(userId: String, time: Long, count: Int): List<UploadedPhoto> {
    uploadedPhotosLocalSource.deleteOldPhotos()

    return getPageInternal(time, count, userId)
      .sortedByDescending { it.photoId }
  }

  private suspend fun getPageInternal(time: Long, count: Int, userId: String): List<UploadedPhoto> {
    val pageOfUploadedPhotos = uploadedPhotosLocalSource.getPage(time, count)
    if (pageOfUploadedPhotos.size == count) {
      return pageOfUploadedPhotos
    }

    val uploadedPhotos = getUploadedPhotosRemoteSource.getPage(userId, time, count)
    if (uploadedPhotos.isEmpty()) {
      return emptyList()
    }

    if (!uploadedPhotosLocalSource.saveMany(uploadedPhotos)) {
      throw DatabaseException("Could not cache uploaded photos in the database")
    }

    return UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhotos(uploadedPhotos)
  }
}