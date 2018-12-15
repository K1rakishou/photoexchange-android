package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.util.PagedApiUtils
import com.kirakishou.photoexchange.helper.util.PhotoAdditionalInfoUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import kotlinx.coroutines.withContext
import net.response.data.ReceivedPhotoResponseData
import timber.log.Timber
import java.util.concurrent.TimeUnit

open class GetReceivedPhotosUseCase(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val pagedApiUtils: PagedApiUtils,
  private val photoAdditionalInfoUtils: PhotoAdditionalInfoUtils,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val blacklistedPhotoRepository: BlacklistedPhotoRepository,
  private val photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetReceivedPhotosUseCase"
  private var lastTimeFreshPhotosCheck = 0L
  private val fiveMinutes = TimeUnit.MINUTES.toMillis(5)

  open suspend fun loadPageOfPhotos(
    forced: Boolean,
    firstUploadedOn: Long,
    lastUploadedOnParam: Long,
    count: Int
  ): Paged<ReceivedPhoto> {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("loadFreshPhotos called")
      val (lastUploadedOn, userId) = getParameters(lastUploadedOnParam)

      val receivedPhotosPage = getPageOfReceivedPhotos(
        forced,
        firstUploadedOn,
        lastUploadedOn,
        userId,
        count
      )

      val receivedPhotosWithInfo = photoAdditionalInfoUtils.appendAdditionalPhotoInfo(
        photoAdditionalInfoRepository,
        apiClient,
        userId,
        receivedPhotosPage.page,
        { receivedPhoto -> receivedPhoto.receivedPhotoName },
        { receivedPhoto, photoAdditionalInfo -> receivedPhoto.copy(photoAdditionalInfo = photoAdditionalInfo) }
      )

      return@withContext Paged(receivedPhotosWithInfo, receivedPhotosPage.isEnd)
    }
  }

  private suspend fun getPageOfReceivedPhotos(
    forced: Boolean,
    firstUploadedOnParam: Long,
    lastUploadedOnParam: Long,
    userIdParam: String,
    countParam: Int
  ): Paged<ReceivedPhoto> {
    if (forced) {
      resetTimer()
    }

    return pagedApiUtils.getPageOfPhotos(
      "received_photos",
      firstUploadedOnParam,
      lastUploadedOnParam,
      countParam,
      userIdParam,
      { firstUploadedOn -> getFreshPhotosCount(userIdParam, firstUploadedOn) },
      { lastUploadedOn, count -> getFromCacheInternal(lastUploadedOn, count) },
      { userId, lastUploadedOn, count -> apiClient.getPageOfReceivedPhotos(userId!!, lastUploadedOn, count) },
      { receivedPhotosRepository.deleteAll() },
      { deleteOldPhotos() },
      { receivedPhotos -> filterBlacklistedPhotos(receivedPhotos) },
      { receivedPhotos ->
        storeInDatabase(receivedPhotos)
        true
      },
      { responseData -> ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhotos(responseData) })
  }

  private fun resetTimer() {
    lastTimeFreshPhotosCheck = 0
  }

  private suspend fun getFromCacheInternal(lastUploadedOn: Long, count: Int): Paged<ReceivedPhoto> {
    //if there is no internet - search only in the database
    val cachedReceivedPhotos = receivedPhotosRepository.getPage(lastUploadedOn, count)
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

  private suspend fun filterBlacklistedPhotos(
    receivedPhotos: List<ReceivedPhotoResponseData>
  ): List<ReceivedPhotoResponseData> {
    return blacklistedPhotoRepository.filterBlacklistedPhotos(receivedPhotos) {
      it.receivedPhotoName
    }
  }

  private suspend fun deleteOldPhotos() {
    database.transactional {
      val oldPhotos = receivedPhotosRepository.findOld()
      Timber.tag(TAG).d("Found ${oldPhotos.size} old received photos")

      for (photo in oldPhotos) {
        uploadedPhotosRepository.deleteByPhotoName(photo.uploadedPhotoName)
        receivedPhotosRepository.deleteByPhotoName(photo.receivedPhotoName)
      }
    }
  }

  private suspend fun storeInDatabase(
    receivedPhotos: List<ReceivedPhotoResponseData>
  ) {
    database.transactional {
      for (receivedPhoto in receivedPhotos) {
        val updateResult = uploadedPhotosRepository.updateReceiverInfo(
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

      if (!receivedPhotosRepository.saveMany(receivedPhotos)) {
        throw DatabaseException("Could not cache received photos in the database")
      }
    }
  }

  private suspend fun getParameters(lastUploadedOn: Long): Pair<Long, String> {
    val time = if (lastUploadedOn != -1L) {
      lastUploadedOn
    } else {
      timeUtils.getTimeFast()
    }

    val userId = settingsRepository.getUserId()
    if (userId.isEmpty()) {
      throw EmptyUserIdException()
    }

    return Pair(time, userId)
  }
}