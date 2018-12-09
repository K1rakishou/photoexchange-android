package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.BlacklistedPhotoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.ReceivedPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.UploadPhotosLocalSource
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.util.PagedApiUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import kotlinx.coroutines.withContext
import net.response.ReceivedPhotosResponse
import timber.log.Timber
import java.util.concurrent.TimeUnit

open class GetReceivedPhotosRepository(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val pagedApiUtils: PagedApiUtils,
  private val receivedPhotosLocalSource: ReceivedPhotosLocalSource,
  private val uploadedPhotosLocalSource: UploadPhotosLocalSource,
  private val blacklistedPhotoLocalSource: BlacklistedPhotoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "GetReceivedPhotosRepository"

  private var lastTimeFreshPhotosCheck = 0L
  private val fiveMinutes = TimeUnit.MINUTES.toMillis(5)

  open suspend fun getPage(
    forced: Boolean,
    firstUploadedOnParam: Long,
    lastUploadedOnParam: Long,
    userIdParam: String,
    countParam: Int
  ): Paged<ReceivedPhoto> {
    return withContext(coroutineContext) {
      if (forced) {
        resetTimer()
      }

      return@withContext pagedApiUtils.getPageOfPhotos(
        "received_photos",
        firstUploadedOnParam,
        lastUploadedOnParam,
        countParam,
        userIdParam, { firstUploadedOn ->
        getFreshPhotosCount(userIdParam, firstUploadedOn)
      }, { lastUploadedOn, count ->
        getFromCacheInternal(lastUploadedOn, count)
      }, { userId, lastUploadedOn, count ->
        apiClient.getPageOfReceivedPhotos(userId!!, lastUploadedOn, count)
      }, {
        deleteAll()
      }, {
        deleteOld()
      }, { receivedPhotos ->
        filterBlacklistedPhotos(receivedPhotos)
      }, { receivedPhotos ->
        storeInDatabase(receivedPhotos)
        true
      }, { responseData ->
        ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhotos(responseData)
      })
    }
  }

  private fun resetTimer() {
    lastTimeFreshPhotosCheck = 0
  }

  private fun getFromCacheInternal(lastUploadedOn: Long, count: Int): Paged<ReceivedPhoto> {
    //if there is no internet - search only in the database
    val cachedReceivedPhotos = receivedPhotosLocalSource.getPage(lastUploadedOn, count)
    return if (cachedReceivedPhotos.size == count) {
      Timber.tag(TAG).d("Found enough received photos in the database")
      Paged(cachedReceivedPhotos, false)
    } else {
      Timber.tag(TAG).d("Found not enough received photos in the database")
      Paged(cachedReceivedPhotos, cachedReceivedPhotos.size < count)
    }
  }

  private suspend fun getFreshPhotosCount(userId: String, firstUploadedOn: Long): Int {
    val now = timeUtils.getTimeFast()

    //if five minutes has passed since we last checked fresh photos count - check again
    return if (now - lastTimeFreshPhotosCheck >= fiveMinutes) {
      lastTimeFreshPhotosCheck = now
      apiClient.getFreshReceivedPhotosCount(userId, firstUploadedOn)
    } else {
      0
    }
  }

  private suspend fun deleteAll() {
    database.transactional {
      receivedPhotosLocalSource.deleteAll()
    }
  }

  private suspend fun deleteOld() {
    database.transactional {
      receivedPhotosLocalSource.deleteOldPhotos()
    }
  }

  private suspend fun filterBlacklistedPhotos(
    receivedPhotos: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>
  ): List<ReceivedPhotosResponse.ReceivedPhotoResponseData> {
    return withContext(coroutineContext) {
      return@withContext blacklistedPhotoLocalSource.filterBlacklistedPhotos(receivedPhotos) {
        it.receivedPhotoName
      }
    }
  }

  private suspend fun storeInDatabase(receivedPhotos: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>) {
    database.transactional {
      for (receivedPhoto in receivedPhotos) {
        val updateResult = uploadedPhotosLocalSource.updateReceiverInfo(
          receivedPhoto.uploadedPhotoName,
          receivedPhoto.receivedPhotoName,
          receivedPhoto.lon,
          receivedPhoto.lat
        )

        if (!updateResult) {
          //no uploaded photo in cached in the database by this name, skip it
          continue
        }
      }

      if (!receivedPhotosLocalSource.saveMany(receivedPhotos)) {
        throw DatabaseException("Could not cache received photos in the database")
      }
    }
  }
}