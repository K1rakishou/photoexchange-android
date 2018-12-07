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
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.helper.util.PagedApiUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

open class GetGalleryPhotosRepository(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val pagedApiUtils: PagedApiUtils,
  private val netUtils: NetUtils,
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource,
  private val galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "GetGalleryPhotosRepository"

  private var lastTimeFreshPhotosCheck = 0L
  private val fiveMinutes = TimeUnit.MINUTES.toMillis(5)

  suspend fun getPage(
    forced: Boolean,
    userId: String,
    firstUploadedOn: Long,
    lastUploadedOn: Long,
    count: Int
  ): Paged<GalleryPhoto> {
    return withContext(coroutineContext) {
      if (forced) {
        resetTimer()
      }

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

  private fun resetTimer() {
    lastTimeFreshPhotosCheck = 0
  }

  private suspend fun getPageOfGalleryPhotos(
    firstUploadedOnParam: Long,
    lastUploadedOnParam: Long,
    countParam: Int
  ): Paged<GalleryPhoto> {
    return pagedApiUtils.getPageOfPhotos(
      "gallery_photos",
      firstUploadedOnParam,
      lastUploadedOnParam,
      countParam,
      null, { firstUploadedOn ->
      getFreshPhotosCount(firstUploadedOn)
    }, { lastUploadedOn, count ->
      getFromCacheInternal(lastUploadedOn, count)
    }, { _, lastUploadedOn, count ->
      apiClient.getPageOfGalleryPhotos(lastUploadedOn, count)
    }, {
      deleteAll()
    }, {
      deleteOld()
    }, { galleryPhotos ->
      galleryPhotoLocalSource.saveMany(galleryPhotos)
    }, { responseData ->
      GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(responseData)
    })
  }

  private suspend fun getFreshPhotosCount(firstUploadedOn: Long): Int {
    val now = timeUtils.getTimeFast()

    //if five minutes has passed since we last checked fresh photos count - check again
    return if (now - lastTimeFreshPhotosCheck >= fiveMinutes) {
      Timber.tag(TAG).d("Enough time has passed since last request")

      lastTimeFreshPhotosCheck = now
      apiClient.getFreshGalleryPhotosCount(firstUploadedOn)
    } else {
      Timber.tag(TAG).d("Not enough time has passed since last request: ${now - lastTimeFreshPhotosCheck} ms")
      0
    }
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

  private suspend fun deleteAll() {
    database.transactional {
      galleryPhotoLocalSource.deleteAll()
      galleryPhotoInfoLocalSource.deleteAll()
    }
  }

  private suspend fun deleteOld() {
    database.transactional {
      galleryPhotoLocalSource.deleteOldPhotos()
      galleryPhotoInfoLocalSource.deleteOldPhotoInfos()
    }
  }

  private suspend fun getGalleryPhotosInfo(userId: String, photoNameList: List<String>): List<GalleryPhotoInfo> {
    if (userId.isEmpty()) {
      Timber.tag(TAG).d("UserId is empty")
      return photoNameList.map { photoName -> GalleryPhotoInfo(photoName, false, false, GalleryPhotoInfo.Type.NoUserId) }
    }

    val cachedGalleryPhotosInfo = galleryPhotoInfoLocalSource.findMany(photoNameList)
    //if there is no wifi - don't even try to access network
    if (!netUtils.allowedToAccessNetwork()) {
      return cachedGalleryPhotosInfo
    }

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