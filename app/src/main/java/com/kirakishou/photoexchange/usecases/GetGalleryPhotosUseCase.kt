package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.BlacklistedPhotoRepository
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.util.PagedApiUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvrx.model.photo.GalleryPhoto
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GetGalleryPhotosUseCase(
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val pagedApiUtils: PagedApiUtils,
  private val getPhotoAdditionalInfoUseCase: GetPhotoAdditionalInfoUseCase,
  private val getFreshPhotosUseCase: GetFreshPhotosUseCase,
  private val galleryPhotosRepository: GalleryPhotosRepository,
  private val blacklistedPhotoRepository: BlacklistedPhotoRepository,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetGalleryPhotosUseCase"

  open suspend fun loadPageOfPhotos(
    forced: Boolean,
    firstUploadedOn: Long?,
    lastUploadedOn: Long?,
    count: Int
  ): Paged<GalleryPhoto> {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("loadPageOfPhotos called")

      val userUuid = settingsRepository.getUserUuid()

      val galleryPhotosPage = getPageOfGalleryPhotos(
        forced,
        firstUploadedOn,
        lastUploadedOn,
        userUuid,
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

  private suspend fun getPageOfGalleryPhotos(
    forced: Boolean,
    firstUploadedOn: Long?,
    lastUploadedOn: Long?,
    userUuidParam: String,
    countParam: Int
  ): Paged<GalleryPhoto> {
    return pagedApiUtils.getPageOfPhotos<GalleryPhoto>(
      "gallery_photos",
      firstUploadedOn,
      lastUploadedOn,
      countParam,
      userUuidParam,
      { lastUploadedOnParam, count -> getFromCacheInternal(lastUploadedOnParam, count) },
      { firstUploadedOnParam -> getFreshPhotosUseCase.getFreshGalleryPhotos(forced, firstUploadedOnParam) },
      { _, lastUploadedOnParam, count ->
        val responseData = apiClient.getPageOfGalleryPhotos(lastUploadedOnParam, count)
        return@getPageOfPhotos GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(
          responseData
        )
      },
      { galleryPhotosRepository.deleteAll() },
      { galleryPhotosRepository.deleteOldPhotos() },
      { galleryPhotos -> filterBlacklistedPhotos(galleryPhotos) },
      { galleryPhotos -> galleryPhotosRepository.saveMany(galleryPhotos) }
    )
  }

  private suspend fun getFromCacheInternal(lastUploadedOn: Long?, count: Int): List<GalleryPhoto> {
    val time = lastUploadedOn ?: timeUtils.getTimePlus26Hours()

    //if there is no internet - search only in the database
    val cachedGalleryPhotos = galleryPhotosRepository.getPage(time, count)
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
}