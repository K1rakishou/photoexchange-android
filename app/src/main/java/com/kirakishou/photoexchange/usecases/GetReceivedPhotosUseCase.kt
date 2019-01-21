package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.EmptyUserUuidException
import com.kirakishou.photoexchange.helper.util.PagedApiUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvrx.model.photo.ReceivedPhoto
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GetReceivedPhotosUseCase(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val pagedApiUtils: PagedApiUtils,
  private val getPhotoAdditionalInfoUseCase: GetPhotoAdditionalInfoUseCase,
  private val getFreshPhotosUseCase: GetFreshPhotosUseCase,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val blacklistedPhotoRepository: BlacklistedPhotoRepository,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetReceivedPhotosUseCase"

  open suspend fun loadPageOfPhotos(
    forced: Boolean,
    firstUploadedOn: Long?,
    lastUploadedOn: Long?,
    count: Int
  ): Paged<ReceivedPhoto> {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("loadFreshPhotos called")

      val userUuid = settingsRepository.getUserUuid()
      if (userUuid.isEmpty()) {
        throw EmptyUserUuidException()
      }

      val receivedPhotosPage = getPageOfReceivedPhotos(
        forced,
        firstUploadedOn,
        lastUploadedOn,
        userUuid,
        count
      )

      val receivedPhotosWithInfo = getPhotoAdditionalInfoUseCase.appendAdditionalPhotoInfo(
        receivedPhotosPage.page,
        { receivedPhoto -> receivedPhoto.receivedPhotoName },
        { receivedPhoto, photoAdditionalInfo -> receivedPhoto.copy(photoAdditionalInfo = photoAdditionalInfo) }
      )

      return@withContext Paged(receivedPhotosWithInfo, receivedPhotosPage.isEnd)
    }
  }

  private suspend fun getPageOfReceivedPhotos(
    forced: Boolean,
    firstUploadedOn: Long?,
    lastUploadedOn: Long?,
    userUuidParam: String,
    countParam: Int
  ): Paged<ReceivedPhoto> {
    return pagedApiUtils.getPageOfPhotos<ReceivedPhoto>(
      "received_photos",
      firstUploadedOn,
      lastUploadedOn,
      countParam,
      userUuidParam,
      { lastUploadedOnParam, count -> getFromCacheInternal(lastUploadedOnParam, count) },
      { firstUploadedOnParam -> getFreshPhotosUseCase.getFreshReceivedPhotos(forced, firstUploadedOnParam) },
      { userUuid, lastUploadedOnParam, count ->
        val responseData = apiClient.getPageOfReceivedPhotos(userUuid!!, lastUploadedOnParam, count)
        return@getPageOfPhotos ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhotos(
          responseData
        )
      },
      { receivedPhotosRepository.deleteAll() },
      { deleteOldPhotos() },
      { receivedPhotos -> filterBlacklistedPhotos(receivedPhotos) },
      { receivedPhotos ->
        storeInDatabase(receivedPhotos)
        true
      })
  }

  private suspend fun getFromCacheInternal(lastUploadedOn: Long?, count: Int): List<ReceivedPhoto> {
    //if there is no internet - search only in the database
    val cachedReceivedPhotos = receivedPhotosRepository.getPage(lastUploadedOn, count)
    return if (cachedReceivedPhotos.size == count) {
      Timber.tag(TAG).d("Found enough received photos in the database")
      cachedReceivedPhotos
    } else {
      Timber.tag(TAG).d("Found not enough received photos in the database")
      cachedReceivedPhotos
    }
  }

  private suspend fun filterBlacklistedPhotos(
    receivedPhotos: List<ReceivedPhoto>
  ): List<ReceivedPhoto> {
    return blacklistedPhotoRepository.filterBlacklistedPhotos(receivedPhotos) {
      it.receivedPhotoName
    }
  }

  private suspend fun deleteOldPhotos() {
    database.transactional {
      val oldPhotos = receivedPhotosRepository.findOld()
      Timber.tag(TAG).d("Found ${oldPhotos.size} old received photos")

      for (photo in oldPhotos) {
        //delete both uploaded and received photos
        uploadedPhotosRepository.deleteByPhotoName(photo.uploadedPhotoName)
        receivedPhotosRepository.deleteByPhotoName(photo.receivedPhotoName)
      }
    }
  }

  private suspend fun storeInDatabase(
    receivedPhotos: List<ReceivedPhoto>
  ) {
    database.transactional {
      for (receivedPhoto in receivedPhotos) {
        val updateResult = uploadedPhotosRepository.updateReceiverInfo(
          receivedPhoto.uploadedPhotoName,
          receivedPhoto.receivedPhotoName,
          receivedPhoto.lonLat.lon,
          receivedPhoto.lonLat.lat
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
}