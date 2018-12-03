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
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhotoInfo
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.util.TimeUtils
import kotlinx.coroutines.withContext
import net.response.GalleryPhotosResponse
import timber.log.Timber

open class GetGalleryPhotosRepository(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource,
  private val galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "GetGalleryPhotosRepository"

  suspend fun getPage(
    userId: String,
    firstUploadedOn: Long,
    lastUploadedOn: Long,
    count: Int
  ): Paged<GalleryPhoto> {
    return withContext(coroutineContext) {
      val pageOfGalleryPhotos = getPageOfGalleryPhotos(firstUploadedOn, lastUploadedOn, count)

      val photoNameList = pageOfGalleryPhotos.page.map { it.photoName }
      val galleryPhotosInfoList = getGalleryPhotosInfo(userId, photoNameList)

      val updatedPhotos = mutableListOf<GalleryPhoto>()
      for (galleryPhoto in pageOfGalleryPhotos.page) {
        val newGalleryPhotoInfo = galleryPhotosInfoList
          .firstOrNull { it.photoName == galleryPhoto.photoName }
          ?: GalleryPhotoInfo(galleryPhoto.photoName, false, false, GalleryPhotoInfo.Type.Normal)

        updatedPhotos += galleryPhoto.copy(galleryPhotoInfo = newGalleryPhotoInfo)
      }

      return@withContext Paged(updatedPhotos, pageOfGalleryPhotos.isEnd)
    }
  }

  private suspend fun getPageOfGalleryPhotos(
    firstUploadedOn: Long,
    lastUploadedOn: Long,
    count: Int
  ): Paged<GalleryPhoto> {
    val freshPhotosCount = try {
      //firstUploadedOn == -1L means that we have not fetched any photos from the server yet so
      //there is no point in checking whether there are fresh photos
      if (firstUploadedOn == -1L) {
        0
      } else {
        apiClient.hasFreshGalleryPhotos(firstUploadedOn)
      }
    } catch (error: ConnectionError) {
      Timber.tag(TAG).e(error)

      //do not attempt to get photos from the server when there is no internet connection
      -1
    }

    val galleryPhotos = if (freshPhotosCount == -1) {
      return getFromCacheInternal(lastUploadedOn, count)
    } else if (freshPhotosCount in 0..count) {
      if (freshPhotosCount == 0) {
        //if there are no fresh photos then we can check the cache
        val fromCache = getFromCacheInternal(lastUploadedOn, count)
        if (fromCache.page.size == count) {
          //if enough photos were found in the cache - return them
          return fromCache
        }

        //if there are no fresh photos and not enough photos were found in the cache -
        //get fresh page from the server
        apiClient.getPageOfGalleryPhotos(lastUploadedOn, count)
      } else {
        //otherwise get fresh photos AND the next page and then combine them
        val photos = mutableListOf<GalleryPhotosResponse.GalleryPhotoResponseData>()

        photos += apiClient.getPageOfGalleryPhotos(timeUtils.getTimeFast(), freshPhotosCount)
        photos += apiClient.getPageOfGalleryPhotos(lastUploadedOn, count)

        photos
      }
    } else {
      //if there are more fresh photos than we have requested - invalidate database cache
      //and start loading photos from the first page
      deleteAll()
      apiClient.getPageOfGalleryPhotos(lastUploadedOn, count)
    }

    if (galleryPhotos.isEmpty()) {
      Timber.tag(TAG).d("No gallery photos were found on the server")
      return Paged(emptyList(), true)
    }

    if (!galleryPhotoLocalSource.saveMany(galleryPhotos)) {
      throw DatabaseException("Could not cache gallery photos in the database")
    }

    val mappedPhotos = GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(galleryPhotos)
    return Paged(mappedPhotos, galleryPhotos.size < count)
  }

  private fun getFromCacheInternal(lastUploadedOn: Long, count: Int): Paged<GalleryPhoto> {
    //if there is no internet - search only in the database
    val cachedGalleryPhotos = galleryPhotoLocalSource.getPage(lastUploadedOn, count)
    return if (cachedGalleryPhotos.size == count) {
      Timber.tag(TAG).d("Found enough gallery photos in the database")
      Paged(cachedGalleryPhotos, false)
    } else {
      Timber.tag(TAG).d("Found not enough gallery photos in the database")
      Paged(cachedGalleryPhotos, cachedGalleryPhotos.size < count)
    }
  }

  //may hang
  private suspend fun deleteAll() {
    database.transactional {
      galleryPhotoLocalSource.deleteAll()
      galleryPhotoInfoLocalSource.deleteAll()
    }
  }

  private suspend fun getGalleryPhotosInfo(userId: String, photoNameList: List<String>): List<GalleryPhotoInfo> {
    if (userId.isEmpty()) {
      Timber.tag(TAG).d("UserId is empty")
      return photoNameList.map { photoName -> GalleryPhotoInfo(photoName, false, false, GalleryPhotoInfo.Type.NoUserId) }
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