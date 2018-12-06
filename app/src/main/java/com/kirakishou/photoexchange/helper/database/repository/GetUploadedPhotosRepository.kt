package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.UploadPhotosLocalSource
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import com.kirakishou.photoexchange.helper.util.PagedApiUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

class GetUploadedPhotosRepository(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val pagedApiUtils: PagedApiUtils,
  private val uploadedPhotosLocalSource: UploadPhotosLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "GetUploadedPhotosRepository"

  private var lastTimeFreshPhotosCheck = 0L
  private val timeToSubOnEveryUserRefreshRequest = TimeUnit.MINUTES.toMillis(2)
  private val fiveMinutes = TimeUnit.MINUTES.toMillis(5)

  suspend fun getPage(
    forced: Boolean,
    firstUploadedOn: Long,
    lastUploadedOn: Long,
    userId: String,
    count: Int
  ): Paged<UploadedPhoto> {
    return withContext(coroutineContext) {
      if (forced) {
        decreaseTimer()
      }

      val uploadedPhotosPage = getPageOfUploadedPhotos(firstUploadedOn, lastUploadedOn, userId, count)
      return@withContext splitPhotos(uploadedPhotosPage)
    }
  }

  private fun decreaseTimer() {
    lastTimeFreshPhotosCheck -= timeToSubOnEveryUserRefreshRequest
    if (lastTimeFreshPhotosCheck < 0) {
      lastTimeFreshPhotosCheck = 0
    }
  }

  private suspend fun getPageOfUploadedPhotos(
    firstUploadedOnParam: Long,
    lastUploadedOnParam: Long,
    userIdParam: String,
    countParam: Int
  ): Paged<UploadedPhoto> {
    return pagedApiUtils.getPageOfPhotos(
      "uploaded_photos",
      firstUploadedOnParam,
      lastUploadedOnParam,
      countParam,
      userIdParam, { firstUploadedOn ->
      getFreshPhotosCount(firstUploadedOn)
    }, { lastUploadedOn, count ->
      getFromCacheInternal(lastUploadedOn, count)
    }, { userId, lastUploadedOn, count ->
      apiClient.getPageOfUploadedPhotos(userId!!, lastUploadedOn, count)
    }, {
      deleteAll()
    }, { uploadedPhotos ->
      uploadedPhotosLocalSource.saveMany(uploadedPhotos)
    }, { responseData ->
      UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhotos(responseData)
    })
  }

  private suspend fun getFreshPhotosCount(firstUploadedOn: Long): Int {
    val now = timeUtils.getTimeFast()

    //if five minutes has passed since we last checked fresh photos count - check again
    return if (now - lastTimeFreshPhotosCheck >= fiveMinutes) {
      lastTimeFreshPhotosCheck = now
      apiClient.getFreshGalleryPhotosCount(firstUploadedOn)
    } else {
      0
    }
  }

  private fun getFromCacheInternal(lastUploadedOn: Long, count: Int): Paged<UploadedPhoto> {
    //if there is no internet - search only in the database
    val cachedGalleryPhotos = uploadedPhotosLocalSource.getPage(lastUploadedOn, count)
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
      uploadedPhotosLocalSource.deleteAll()
    }
  }

  private fun splitPhotos(uploadedPhotosPage: Paged<UploadedPhoto>): Paged<UploadedPhoto> {
    val uploadedPhotosWithNoReceiver = uploadedPhotosPage.page
      .filter { it.receiverInfo == null }

    val uploadedPhotosWithReceiver = uploadedPhotosPage.page
      .filter { it.receiverInfo != null }

    //we need to show photos without receiver first and after them photos with receiver
    return Paged(uploadedPhotosWithNoReceiver + uploadedPhotosWithReceiver, uploadedPhotosPage.isEnd)
  }
}