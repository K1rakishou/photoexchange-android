package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosInfoMapper
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoInfoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoLocalSource
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhotoInfo
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GetGalleryPhotosRepository(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource,
  private val galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "GetGalleryPhotosRepository"

  suspend fun getPage(userId: String, time: Long, count: Int): Paged<GalleryPhoto> {
    return withContext(coroutineContext) {
      val pageOfGalleryPhotos = getPageOfGalleryPhotos(time, count)

      val photoNameList = pageOfGalleryPhotos.page.map { it.photoName }
      val galleryPhotosInfoList = getGalleryPhotosInfo(userId, photoNameList)

      val updatedPhotos = mutableListOf<GalleryPhoto>()
      for (galleryPhoto in pageOfGalleryPhotos.page) {
        val newGalleryPhotoInfo = galleryPhotosInfoList
          .firstOrNull { it.photoName == galleryPhoto.photoName }
          ?: GalleryPhotoInfo(galleryPhoto.photoName, false, false, GalleryPhotoInfo.Type.Normal)

        updatedPhotos += galleryPhoto.copy(galleryPhotoInfo = newGalleryPhotoInfo)
      }

      val sortedPhotos = updatedPhotos
        .sortedByDescending { it.uploadedOn }

      return@withContext Paged(sortedPhotos, pageOfGalleryPhotos.isEnd)
    }
  }

  private suspend fun getPageOfGalleryPhotos(time: Long, count: Int): Paged<GalleryPhoto> {
    //if we found exactly the same amount of gallery photos that was requested - return them
    val cachedGalleryPhotos = galleryPhotoLocalSource.getPage(time, count)
    if (cachedGalleryPhotos.size == count) {
      Timber.tag(TAG).d("Found enough gallery photos in the database")
      return Paged(cachedGalleryPhotos, false)
    }

    //otherwise reload the page from the server
    val galleryPhotos = apiClient.getPageOfGalleryPhotos(time, count)
    if (galleryPhotos.isEmpty()) {
      Timber.tag(TAG).d("No gallery photos were found on the server")
      return Paged(cachedGalleryPhotos, true)
    }

    //TODO: filter out duplicates here?
    if (!galleryPhotoLocalSource.saveMany(galleryPhotos)) {
      throw DatabaseException("Could not cache gallery photos in the database")
    }

    val mappedPhotos = GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(galleryPhotos)
    return Paged(mappedPhotos, mappedPhotos.size < count)
  }

  private suspend fun getGalleryPhotosInfo(userId: String, photoNameList: List<String>): List<GalleryPhotoInfo> {
    if (userId.isEmpty()) {
      Timber.tag(TAG).d("UserId is empty")
      return photoNameList.
        map { photoName -> GalleryPhotoInfo(photoName, false, false, GalleryPhotoInfo.Type.NoUserId) }
    }

    val cachedGalleryPhotosInfo = galleryPhotoInfoLocalSource.findMany(photoNameList)
    if (cachedGalleryPhotosInfo.size == photoNameList.size) {
      Timber.tag(TAG).d("Found enough gallery photo infos in the database")
      return cachedGalleryPhotosInfo
    }

    val requestString = photoNameList.joinToString(separator = Constants.PHOTOS_SEPARATOR)
    val galleryPhotosInfo = apiClient.getGalleryPhotoInfo(userId, requestString)
    if (galleryPhotosInfo.isEmpty()) {
      Timber.tag(TAG).d("No gallery photo info were found on the server")
      return emptyList()
    }

    if (!galleryPhotoInfoLocalSource.saveMany(galleryPhotosInfo)) {
      throw DatabaseException("Could not cache gallery photo info in the database")
    }

    return GalleryPhotosInfoMapper.FromResponseData.ToObject.toGalleryPhotoInfoList(galleryPhotosInfo)
  }

}