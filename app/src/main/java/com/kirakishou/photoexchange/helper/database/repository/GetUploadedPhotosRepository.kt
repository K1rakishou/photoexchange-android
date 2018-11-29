package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.UploadPhotosLocalSource
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import kotlinx.coroutines.withContext
import timber.log.Timber

class GetUploadedPhotosRepository(
  private val uploadedPhotosLocalSource: UploadPhotosLocalSource,
  private val apiClient: ApiClient,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "GetUploadedPhotosRepository"

  suspend fun getPage(userId: String, time: Long, count: Int): List<UploadedPhoto> {
    return withContext(coroutineContext) {
      uploadedPhotosLocalSource.deleteOldPhotos()

      val uploadedPhotos = getPageInternal(time, count, userId)
      val uploadedPhotosWithNoReceiver = uploadedPhotos
        .filter { it.receiverInfo == null }
        .sortedByDescending { it.uploadedOn }

      val uploadedPhotosWithReceiver = uploadedPhotos
        .filter { it.receiverInfo != null }
        .sortedByDescending { it.uploadedOn }

      //we need to show photos without receiver first and after them photos with receiver
      return@withContext uploadedPhotosWithNoReceiver + uploadedPhotosWithReceiver
    }
  }

  private suspend fun getPageInternal(time: Long, count: Int, userId: String): List<UploadedPhoto> {
    val pageOfUploadedPhotos = uploadedPhotosLocalSource.getPage(time, count)
    if (pageOfUploadedPhotos.size == count) {
      Timber.tag(TAG).d("Found enough uploaded photos in the database")
      return pageOfUploadedPhotos
    }

    Timber.tag(TAG).d("Trying to find uploaded photos on the server")

    //TODO: the method may be called AFTER a photo has been uploaded and it will contain receiveInfo
    //so we need to check whether it contains it and if it does, we need to notify the ReceivedPhotosFragment about it

    val uploadedPhotos = apiClient.getPageOfUploadedPhotos(userId, time, count)
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