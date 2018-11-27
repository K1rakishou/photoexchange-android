package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosInfoMapper
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoInfoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoLocalSource
import com.kirakishou.photoexchange.helper.database.source.remote.GalleryPhotoInfoRemoteSource
import com.kirakishou.photoexchange.helper.database.source.remote.GalleryPhotoRemoteSource
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhotoInfo
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import kotlinx.coroutines.withContext

open class GetGalleryPhotosRepository(
  private val database: MyDatabase,
  private val galleryPhotoRemoteSource: GalleryPhotoRemoteSource,
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource,
  private val galleryPhotoInfoRemoteSource: GalleryPhotoInfoRemoteSource,
  private val galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "GetGalleryPhotosRepository"

  suspend fun getPage(userId: String, time: Long, count: Int): List<GalleryPhoto> {
    return withContext(coroutineContext) {
      deleteOld()

      val galleryPhotos = getPageOfGalleryPhotos(time, count).toMutableList()
      val galleryPhotosInfoList = getGalleryPhotosInfo(userId, galleryPhotos.map { it.photoName })

      if (galleryPhotosInfoList.isNotEmpty()) {
        for ((index, galleryPhoto) in galleryPhotos.withIndex()) {
          val newGalleryPhotoInfo = galleryPhotosInfoList
            .firstOrNull { it.photoName == galleryPhoto.photoName }
            ?: GalleryPhotoInfo.empty()

          galleryPhotos[index] = galleryPhoto.copy(galleryPhotoInfo = newGalleryPhotoInfo)
        }
      }

      return@withContext galleryPhotos
        .sortedByDescending { it.uploadedOn }
    }
  }

  private suspend fun deleteOld() {
    database.transactional {
      galleryPhotoLocalSource.deleteOld()
      galleryPhotoInfoLocalSource.deleteOld()
    }
  }

  private suspend fun getPageOfGalleryPhotos(time: Long, count: Int): List<GalleryPhoto> {
    //if we found exactly the same amount of gallery photos that was requested - return them
    val cachedGalleryPhotos = galleryPhotoLocalSource.getPage(time, count)
    if (cachedGalleryPhotos.size == count) {
      return cachedGalleryPhotos
    }

    //otherwise reload the page from the server
    val galleryPhotos = galleryPhotoRemoteSource.getPageOfGalleryPhotos(time, count)
    if (galleryPhotos.isEmpty()) {
      return cachedGalleryPhotos
    }

    if (!galleryPhotoLocalSource.saveMany(galleryPhotos)) {
      throw DatabaseException("Could not cache gallery photos in the database")
    }

    return GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(galleryPhotos)
  }

  private suspend fun getGalleryPhotosInfo(userId: String, photoNameList: List<String>): List<GalleryPhotoInfo> {
    val cachedGalleryPhotosInfo = galleryPhotoInfoLocalSource.findMany(photoNameList)
    if (cachedGalleryPhotosInfo.size == photoNameList.size) {
      return cachedGalleryPhotosInfo
    }

    val requestString = photoNameList.joinToString(separator = Constants.PHOTOS_SEPARATOR)
    val galleryPhotosInfo = galleryPhotoInfoRemoteSource.getGalleryPhotoInfo(userId, requestString)
    if (galleryPhotosInfo.isEmpty()) {
      return emptyList()
    }

    if (!galleryPhotoInfoLocalSource.saveMany(galleryPhotosInfo)) {
      throw DatabaseException("Could not cache gallery photo info in the database")
    }

    return GalleryPhotosInfoMapper.FromResponseData.ToObject.toGalleryPhotoInfoList(galleryPhotosInfo)
  }

}