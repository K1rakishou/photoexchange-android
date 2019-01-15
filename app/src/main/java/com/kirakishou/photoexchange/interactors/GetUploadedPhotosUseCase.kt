package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.EmptyUserUuidException
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
  private val getFreshPhotosUseCase: GetFreshPhotosUseCase,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetUploadedPhotosUseCase"

  open suspend fun loadPageOfPhotos(
    forced: Boolean,
    firstUploadedOn: Long,
    lastUploadedOnParam: Long,
    count: Int
  ): Paged<UploadedPhoto> {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("loadPageOfPhotos called")

      val (lastUploadedOn, userUuid) = getParameters(lastUploadedOnParam)

      val uploadedPhotosPage = getPageOfUploadedPhotos(
        forced,
        firstUploadedOn,
        lastUploadedOn,
        userUuid,
        count
      )

      return@withContext splitPhotos(uploadedPhotosPage)
    }
  }

  private suspend fun getPageOfUploadedPhotos(
    forced: Boolean,
    firstUploadedOnParam: Long,
    lastUploadedOnParam: Long,
    userUuidParam: String,
    countParam: Int
  ): Paged<UploadedPhoto> {
    return pagedApiUtils.getPageOfPhotos(
      "uploaded_photos",
      firstUploadedOnParam,
      lastUploadedOnParam,
      countParam,
      userUuidParam,
      { lastUploadedOn, count -> getFromCacheInternal(lastUploadedOn, count) },
      { userUuid, firstUploadedOn -> getFreshPhotosUseCase.getFreshUploadedPhotos(userUuid!!, forced, firstUploadedOn) },
      { userUuid, lastUploadedOn, count -> apiClient.getPageOfUploadedPhotos(userUuid!!, lastUploadedOn, count) },
      { uploadedPhotosRepository.deleteAll() },
      {
        //do not delete uploaded photos from this use case, do it in the received photo use case
      },
      { responseData -> UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhotos(responseData) },
      { uploadedPhotos ->
        //we don't need to filter uploaded photos
        uploadedPhotos
      },
      { uploadedPhotos -> uploadedPhotosRepository.saveMany(uploadedPhotos) }
    )
  }

  private suspend fun getFromCacheInternal(lastUploadedOn: Long, count: Int): List<UploadedPhoto> {
    //if there is no internet - search only in the database
    val cachedUploadedPhotos = uploadedPhotosRepository.getPage(lastUploadedOn, count)
    return if (cachedUploadedPhotos.size == count) {
      Timber.tag(TAG).d("Found enough uploaded photos in the database")
      cachedUploadedPhotos
    } else {
      Timber.tag(TAG).d("Found not enough uploaded photos in the database")
      cachedUploadedPhotos
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

    val userUuid = settingsRepository.getUserUuid()
    if (userUuid.isEmpty()) {
      throw EmptyUserUuidException()
    }

    return Pair(time, userUuid)
  }
}