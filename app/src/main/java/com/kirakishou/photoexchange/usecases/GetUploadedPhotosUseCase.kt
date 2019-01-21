package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.EmptyUserUuidException
import com.kirakishou.photoexchange.helper.util.PagedApiUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvrx.model.photo.UploadedPhoto
import kotlinx.coroutines.withContext
import timber.log.Timber

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
    firstUploadedOn: Long?,
    lastUploadedOn: Long?,
    count: Int
  ): Paged<UploadedPhoto> {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("loadPageOfPhotos called")

      val userUuid = settingsRepository.getUserUuid()
      if (userUuid.isEmpty()) {
        throw EmptyUserUuidException()
      }

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
    firstUploadedOn: Long?,
    lastUploadedOn: Long?,
    userUuidParam: String,
    countParam: Int
  ): Paged<UploadedPhoto> {
    return pagedApiUtils.getPageOfPhotos<UploadedPhoto>(
      "uploaded_photos",
      firstUploadedOn,
      lastUploadedOn,
      countParam,
      userUuidParam,
      { lastUploadedOnParam, count -> getFromCacheInternal(lastUploadedOnParam, count) },
      { firstUploadedOnParam -> getFreshPhotosUseCase.getFreshUploadedPhotos(forced, firstUploadedOnParam) },
      { userUuid, lastUploadedOnParam, count ->
        val responseData = apiClient.getPageOfUploadedPhotos(userUuid!!, lastUploadedOnParam, count)
        return@getPageOfPhotos UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhotos(responseData)
      },
      { uploadedPhotosRepository.deleteAll() },
      {
        //do not delete uploaded photos from this use case, do it in the get received photos use case
      },
      { uploadedPhotos ->
        //we don't need to filter uploaded photos
        uploadedPhotos
      },
      { uploadedPhotos -> uploadedPhotosRepository.saveMany(uploadedPhotos) }
    )
  }

  private suspend fun getFromCacheInternal(lastUploadedOn: Long?, count: Int): List<UploadedPhoto> {
    val time = lastUploadedOn ?: timeUtils.getTimePlus26Hours()

    //if there is no internet - search only in the database
    val cachedUploadedPhotos = uploadedPhotosRepository.getPage(time, count)
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
}