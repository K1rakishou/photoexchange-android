package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.BlacklistedPhotoRepository
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.util.PagedApiUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

open class GetGalleryPhotosUseCase(
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val pagedApiUtils: PagedApiUtils,
  private val getPhotoAdditionalInfoUseCase: GetPhotoAdditionalInfoUseCase,
  private val galleryPhotosRepository: GalleryPhotosRepository,
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

      val galleryPhotoWithInfo = getPhotoAdditionalInfoUseCase.appendAdditionalPhotoInfo(
        galleryPhotosPage.page,
        { galleryPhoto -> galleryPhoto.photoName },
        { galleryPhoto, photoAdditionalInfo -> galleryPhoto.copy(photoAdditionalInfo = photoAdditionalInfo) }
      )

      return@withContext Paged(galleryPhotoWithInfo, galleryPhotosPage.isEnd)
    }
  }

  @Synchronized
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
      { responseData -> GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(responseData) },
      { galleryPhotos -> filterBlacklistedPhotos(galleryPhotos) },
      { galleryPhotos -> galleryPhotosRepository.saveMany(galleryPhotos) }
    )
  }

  private suspend fun getFreshPhotosCount(firstUploadedOn: Long): Int {
    //if five minutes has passed since we last checked fresh photos count - check again
    val shouldMakeRequest = synchronized(GetGalleryPhotosUseCase::class) {
      val now = timeUtils.getTimeFast()
      if (now - lastTimeFreshPhotosCheck >= fiveMinutes) {
        lastTimeFreshPhotosCheck = now
        true
      } else {
        false
      }
    }

    if (!shouldMakeRequest) {
      return 0
    }

    return apiClient.getFreshGalleryPhotosCount(firstUploadedOn)
  }

  private suspend fun getFromCacheInternal(lastUploadedOn: Long, count: Int): List<GalleryPhoto> {
    //if there is no internet - search only in the database
    val cachedGalleryPhotos = galleryPhotosRepository.getPage(lastUploadedOn, count)
    return if (cachedGalleryPhotos.size == count) {
      Timber.tag(TAG).d("Found enough gallery photos in the database")
      cachedGalleryPhotos
    } else {
      Timber.tag(TAG).d("Found not enough gallery photos in the database")
      cachedGalleryPhotos
    }
  }

  private suspend fun filterBlacklistedPhotos(
    receivedPhotos: List<GalleryPhoto>
  ): List<GalleryPhoto> {
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
    val userId = settingsRepository.getUserUuid()
    return lastUploadedOn to userId
  }
}