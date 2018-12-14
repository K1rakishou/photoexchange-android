package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.BlacklistedPhotoRepository
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotoAdditionalInfoRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.helper.util.PagedApiUtils
import com.kirakishou.photoexchange.helper.util.PhotoAdditionalInfoUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import kotlinx.coroutines.withContext
import net.response.data.GalleryPhotoResponseData
import timber.log.Timber
import java.util.concurrent.TimeUnit

open class GetGalleryPhotosUseCase(
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val pagedApiUtils: PagedApiUtils,
  private val photoAdditionalInfoUtils: PhotoAdditionalInfoUtils,
  private val netUtils: NetUtils,
  private val galleryPhotosRepository: GalleryPhotosRepository,
  private val photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
  private val blacklistedPhotoRepository: BlacklistedPhotoRepository,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetGalleryPhotosUseCase"

  private var lastTimeFreshPhotosCheck = 0L
  private val fiveMinutes = TimeUnit.MINUTES.toMillis(5)

  open suspend fun loadPageOfPhotos(
    forced: Boolean,
    firstUploadedOn: Long,
    lastUploadedOnParam: Long,
    count: Int
  ): Paged<GalleryPhoto> {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("loadPageOfPhotos called")

      val (lastUploadedOn, userId) = getParameters(lastUploadedOnParam)

      if (forced) {
        resetTimer()
      }

      val galleryPhotosPage = getPageOfGalleryPhotos(
        firstUploadedOn,
        lastUploadedOn,
        userId,
        count
      )

      val galleryPhotoWithInfo = photoAdditionalInfoUtils.appendAdditionalPhotoInfo(
        photoAdditionalInfoRepository,
        apiClient,
        userId,
        galleryPhotosPage.page,
        { galleryPhoto -> galleryPhoto.photoName },
        { galleryPhoto, photoAdditionalInfo -> galleryPhoto.copy(photoAdditionalInfo = photoAdditionalInfo) }
      )

      return@withContext Paged(galleryPhotoWithInfo, galleryPhotosPage.isEnd)
    }
  }

  private fun resetTimer() {
    lastTimeFreshPhotosCheck = 0
  }

  private suspend fun getPageOfGalleryPhotos(
    firstUploadedOnParam: Long,
    lastUploadedOnParam: Long,
    userIdParam: String,
    countParam: Int
  ): Paged<GalleryPhoto> {
    return pagedApiUtils.getPageOfPhotos(
      "gallery_photos",
      firstUploadedOnParam,
      lastUploadedOnParam,
      countParam,
      userIdParam,
      { firstUploadedOn -> getFreshPhotosCount(firstUploadedOn) },
      { lastUploadedOn, count -> getFromCacheInternal(lastUploadedOn, count) },
      { _, lastUploadedOn, count -> apiClient.getPageOfGalleryPhotos(lastUploadedOn, count) },
      { galleryPhotosRepository.deleteAll() },
      { galleryPhotosRepository.deleteOldPhotos() },
      { galleryPhotos -> filterBlacklistedPhotos(galleryPhotos) },
      { galleryPhotos -> galleryPhotosRepository.saveMany(galleryPhotos) },
      { responseData -> GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(responseData) }
    )
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

  private suspend fun getFromCacheInternal(lastUploadedOn: Long, count: Int): Paged<GalleryPhoto> {
    //if there is no internet - search only in the database
    val cachedGalleryPhotos = galleryPhotosRepository.getPage(lastUploadedOn, count)
    return if (cachedGalleryPhotos.size == count) {
      Timber.tag(TAG).d("Found enough gallery photos in the database")
      Paged(cachedGalleryPhotos, false)
    } else {
      Timber.tag(TAG).d("Found not enough gallery photos in the database")
      Paged(cachedGalleryPhotos, cachedGalleryPhotos.size < count)
    }
  }

  private suspend fun filterBlacklistedPhotos(
    receivedPhotos: List<GalleryPhotoResponseData>
  ): List<GalleryPhotoResponseData> {
    return blacklistedPhotoRepository.filterBlacklistedPhotos(receivedPhotos) {
      it.photoName
    }
  }

  private suspend fun getParameters(lastUploadedOnParam: Long): Pair<Long, String> {
    val lastUploadedOn = if (lastUploadedOnParam != -1L) {
      lastUploadedOnParam
    } else {
      timeUtils.getTimeFast()
    }

    //empty userId is allowed here since we need it only when fetching photoAdditionalInfo
    val userId = settingsRepository.getUserId()
    return lastUploadedOn to userId
  }
}