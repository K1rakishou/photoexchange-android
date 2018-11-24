package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.UploadPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.remote.GetUploadedPhotosRemoteSource
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.DatabaseException
import kotlinx.coroutines.withContext
import timber.log.Timber

class GetUploadedPhotosRepository(
  private val uploadedPhotosLocalSource: UploadPhotosLocalSource,
  private val getUploadedPhotosRemoteSource: GetUploadedPhotosRemoteSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "GetUploadedPhotosRepository"

  suspend fun getPage(userId: String, time: Long, count: Int): List<UploadedPhoto> {
    return withContext(coroutineContext) {
      uploadedPhotosLocalSource.deleteOldPhotos()

      return@withContext getPageInternal(time, count, userId)
        .sortedByDescending { it.photoId }
    }
  }

  private suspend fun getPageInternal(time: Long, count: Int, userId: String): List<UploadedPhoto> {
    val pageOfUploadedPhotos = uploadedPhotosLocalSource.getPage(time, count)
    if (pageOfUploadedPhotos.size == count) {
      Timber.tag(TAG).d("Found enough uploaded photos in the database")
      return pageOfUploadedPhotos
    }

    Timber.tag(TAG).d("Trying to find uploaded photos on the server")

    val uploadedPhotos = getUploadedPhotosRemoteSource.getPage(userId, time, count)
    if (uploadedPhotos.isEmpty()) {
      Timber.tag(TAG).d("No uploaded photos were found on the server")
      return emptyList()
    }

    Timber.tag(TAG).d("Found ${uploadedPhotos.size} uploaded photos on the server")

    if (!uploadedPhotosLocalSource.saveMany(uploadedPhotos)) {
      throw DatabaseException("Could not cache uploaded photos in the database")
    }

    return UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhotos(uploadedPhotos)
  }
}