package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.util.PagedApiUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

open class GetUploadedPhotosUseCase(
  private val apiClient: ApiClient,
  private val pagedApiUtils: PagedApiUtils,
  private val timeUtils: TimeUtils,
  private val settingsRepository: SettingsRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetUploadedPhotosUseCase"
  private var lastTimeFreshPhotosCheck = 0L
  private val fiveMinutes = TimeUnit.MINUTES.toMillis(5)

  open suspend fun loadPageOfPhotos(
    forced: Boolean,
    firstUploadedOn: Long,
    lastUploadedOnParam: Long,
    count: Int
  ): Paged<UploadedPhoto> {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("loadPageOfPhotos called")
      val (lastUploadedOn, userId) = getParameters(lastUploadedOnParam)

      return@withContext getPage(
        forced,
        firstUploadedOn,
        lastUploadedOn,
        userId,
        count
      )
    }
  }

  suspend fun getPage(
    forced: Boolean,
    firstUploadedOn: Long,
    lastUploadedOn: Long,
    userId: String,
    count: Int
  ): Paged<UploadedPhoto> {
    if (forced) {
      resetTimer()
    }

    val uploadedPhotosPage = getPageOfUploadedPhotos(firstUploadedOn, lastUploadedOn, userId, count)
    return splitPhotos(uploadedPhotosPage)
  }

  private fun resetTimer() {
    lastTimeFreshPhotosCheck = 0
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
      userIdParam,
      { firstUploadedOn -> getFreshPhotosCount(userIdParam, firstUploadedOn) },
      { lastUploadedOn, count -> getFromCacheInternal(lastUploadedOn, count) },
      { userId, lastUploadedOn, count -> apiClient.getPageOfUploadedPhotos(userId!!, lastUploadedOn, count) },
      { uploadedPhotosRepository.deleteAll() },
      {
        //do not delete uploaded photos from this use case, do it in the received photo use case
      },
      { uploadedPhotos ->
        //we don't need to filter uploaded photos
        uploadedPhotos
      },
      { uploadedPhotos -> uploadedPhotosRepository.saveMany(uploadedPhotos) },
      { responseData -> UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhotos(responseData) }
    )
  }

  private suspend fun getFreshPhotosCount(userId: String, firstUploadedOn: Long): Int {
    val now = timeUtils.getTimeFast()

    //if five minutes has passed since we last checked fresh photos count - check again
    return if (now - lastTimeFreshPhotosCheck >= fiveMinutes) {
      lastTimeFreshPhotosCheck = now
      apiClient.getFreshUploadedPhotosCount(userId, firstUploadedOn)
    } else {
      0
    }
  }

  private suspend fun getFromCacheInternal(lastUploadedOn: Long, count: Int): Paged<UploadedPhoto> {
    //if there is no internet - search only in the database
    val cachedUploadedPhotos = uploadedPhotosRepository.getPage(lastUploadedOn, count)
    return if (cachedUploadedPhotos.size == count) {
      Timber.tag(TAG).d("Found enough uploaded photos in the database")
      Paged(cachedUploadedPhotos, false)
    } else {
      Timber.tag(TAG).d("Found not enough uploaded photos in the database")
      Paged(cachedUploadedPhotos, cachedUploadedPhotos.size < count)
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