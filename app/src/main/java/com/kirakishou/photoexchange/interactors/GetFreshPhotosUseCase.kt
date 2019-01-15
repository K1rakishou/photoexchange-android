package com.kirakishou.photoexchange.interactors

import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.exception.AttemptToAccessInternetWithMeteredNetworkException
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import java.util.concurrent.TimeUnit

class GetFreshPhotosUseCase(
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetFreshPhotosUseCase"
  private val lastTimeFreshPhotosCheckMap = hashMapOf<PhotoType, Long>()
  private val timeBetweenFreshPhotosCheck by lazy {
    if (BuildConfig.DEBUG) {
      1
    } else {
      TimeUnit.MINUTES.toMillis(1)
    }
  }

  init {
    lastTimeFreshPhotosCheckMap.put(PhotoType.Uploaded, 0L)
    lastTimeFreshPhotosCheckMap.put(PhotoType.Received, 0L)
    lastTimeFreshPhotosCheckMap.put(PhotoType.Gallery, 0L)
  }

  @Throws(AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun getFreshUploadedPhotos(userUuid: String, forced: Boolean, firstUploadedOn: Long): List<UploadedPhoto> {
    if (firstUploadedOn <= 0) {
      return emptyList()
    }

    if (forced) {
      resetTimer(PhotoType.Uploaded)
    }

    val freshPhotosCount = getFreshPhotosCount(PhotoType.Uploaded, userUuid, firstUploadedOn)
    if (freshPhotosCount == 0) {
      return emptyList()
    }

    return apiClient.getPageOfUploadedPhotos(userUuid, timeUtils.getTimeFast(), freshPhotosCount)
      .map { responseData -> UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhoto(responseData) }
  }

  @Throws(AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun getFreshReceivedPhotos(userUuid: String, forced: Boolean, firstUploadedOn: Long): List<ReceivedPhoto> {
    if (firstUploadedOn <= 0) {
      return emptyList()
    }

    if (forced) {
      resetTimer(PhotoType.Received)
    }

    val freshPhotosCount = getFreshPhotosCount(PhotoType.Received, userUuid, firstUploadedOn)
    if (freshPhotosCount == 0) {
      return emptyList()
    }

    return apiClient.getPageOfReceivedPhotos(userUuid, timeUtils.getTimeFast(), freshPhotosCount)
      .map { responseData -> ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhoto(responseData) }
  }

  @Throws(AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun getFreshGalleryPhotos(forced: Boolean, firstUploadedOn: Long): List<GalleryPhoto> {
    if (firstUploadedOn <= 0) {
      return emptyList()
    }

    if (forced) {
      resetTimer(PhotoType.Gallery)
    }

    val freshPhotosCount = getFreshPhotosCount(PhotoType.Gallery, null, firstUploadedOn)
    if (freshPhotosCount == 0) {
      return emptyList()
    }

    return apiClient.getPageOfGalleryPhotos(timeUtils.getTimeFast(), freshPhotosCount)
      .map { responseData -> GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhoto(responseData) }
  }

  @Synchronized
  private fun resetTimer(photoType: PhotoType) {
    lastTimeFreshPhotosCheckMap[photoType] = 0L
  }

  private suspend fun getFreshPhotosCount(photoType: PhotoType, userUuid: String?, firstUploadedOn: Long): Int {
    val shouldMakeRequest = synchronized(GetGalleryPhotosUseCase::class) {
      val now = timeUtils.getTimeFast()

      /**
       * if [timeBetweenFreshPhotosCheck] time has passed since we last checked fresh photos count - check again
       * */
      if (now - lastTimeFreshPhotosCheckMap[photoType]!! >= timeBetweenFreshPhotosCheck) {
        lastTimeFreshPhotosCheckMap[photoType] = now
        true
      } else {
        false
      }
    }

    if (!shouldMakeRequest) {
      return 0
    }

    return when (photoType) {
      PhotoType.Uploaded -> apiClient.getFreshUploadedPhotosCount(userUuid!!, firstUploadedOn)
      PhotoType.Received -> apiClient.getFreshReceivedPhotosCount(userUuid!!, firstUploadedOn)
      PhotoType.Gallery -> apiClient.getFreshGalleryPhotosCount(firstUploadedOn)
    }
  }

  enum class PhotoType {
    Uploaded,
    Received,
    Gallery
  }
}