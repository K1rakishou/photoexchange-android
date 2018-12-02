package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.Paged
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

  /**
   * This method skips the database cache
   * */
  suspend fun getFresh(time: Long, count: Int, userId: String): Paged<UploadedPhoto> {
    return withContext(coroutineContext) {
      //get a page of fresh photos from the server
      val uploadedPhotos = apiClient.getPageOfUploadedPhotos(userId, time, count)
      if (uploadedPhotos.isEmpty()) {
        Timber.tag(TAG).d("No fresh uploaded photos were found on the server")
        return@withContext Paged(emptyList<UploadedPhoto>(), true)
      }

      if (!uploadedPhotosLocalSource.saveMany(uploadedPhotos)) {
        throw DatabaseException("Could not cache fresh uploaded photos in the database")
      }

      val mappedPhotos = UploadedPhotosMapper.FromResponse.ToObject
        .toUploadedPhotos(uploadedPhotos)

      val photos = splitPhotos(mappedPhotos)
      return@withContext Paged(photos, photos.size < count)
    }
  }

  /**
   * This method includes photos from the database cache
   * */
  suspend fun getPage(time: Long, count: Int, userId: String): Paged<UploadedPhoto> {
    return withContext(coroutineContext) {
      val pageOfUploadedPhotos = uploadedPhotosLocalSource.getPage(time, count)
      if (pageOfUploadedPhotos.size == count) {
        Timber.tag(TAG).d("Found enough uploaded photos in the database")
        return@withContext Paged(pageOfUploadedPhotos, false)
      }

      val uploadedPhotos = apiClient.getPageOfUploadedPhotos(userId, time, count)
      if (uploadedPhotos.isEmpty()) {
        Timber.tag(TAG).d("No uploaded photos were found on the server")
        return@withContext Paged(pageOfUploadedPhotos, true)
      }

      //TODO: filter out duplicates here?
      if (!uploadedPhotosLocalSource.saveMany(uploadedPhotos)) {
        throw DatabaseException("Could not cache uploaded photos in the database")
      }

      val mappedPhotos = UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhotos(uploadedPhotos)
      val photos = splitPhotos(mappedPhotos)

      return@withContext Paged(photos, photos.size < count)
    }
  }

  private fun splitPhotos(uploadedPhotos: List<UploadedPhoto>): List<UploadedPhoto> {
    val uploadedPhotosWithNoReceiver = uploadedPhotos
      .filter { it.receiverInfo == null }
      .sortedByDescending { it.uploadedOn }

    val uploadedPhotosWithReceiver = uploadedPhotos
      .filter { it.receiverInfo != null }
      .sortedByDescending { it.uploadedOn }

    //we need to show photos without receiver first and after them photos with receiver
    return uploadedPhotosWithNoReceiver + uploadedPhotosWithReceiver
  }
}