package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.PhotoAdditionalInfoMapper
import com.kirakishou.photoexchange.helper.database.repository.PhotoAdditionalInfoRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.mvp.model.photo.PhotoAdditionalInfo
import timber.log.Timber

class PhotoAdditionalInfoUtilsImpl(
  private val netUtils: NetUtils
) : PhotoAdditionalInfoUtils {
  private val TAG = "PhotoAdditionalInfoUtilsImpl"

  //TODO: deleteOld
  override suspend fun <T> appendAdditionalPhotoInfo(
    photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
    apiClient: ApiClient,
    userId: String,
    galleryPhotos: List<T>,
    photoNameSelectorFunc: (T) -> String,
    copyFunc: (T, PhotoAdditionalInfo) -> T
  ): List<T> {
    val additionalPhotoInfoList = getPhotoAdditionalInfoForGalleryPhotos(
      userId,
      galleryPhotos.map { photoNameSelectorFunc(it) },
      photoAdditionalInfoRepository,
      apiClient
    )

    if (additionalPhotoInfoList == null) {
      return galleryPhotos
    }

    val resultList = mutableListOf<T>()

    for (galleryPhoto in galleryPhotos) {
      val photoName = photoNameSelectorFunc(galleryPhoto)

      val photoAdditionalInfo = additionalPhotoInfoList
        .firstOrNull { it.photoName == photoName }

      val info = (photoAdditionalInfo ?: PhotoAdditionalInfo.empty(photoName))
        .copy(hasUserId = userId.isNotEmpty())

      resultList += copyFunc(galleryPhoto, info)
    }

    return resultList
  }

  private suspend fun getPhotoAdditionalInfoForGalleryPhotos(
    userId: String,
    photoNameList: List<String>,
    photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
    apiClient: ApiClient
  ): List<PhotoAdditionalInfo>? {
    if (userId.isEmpty()) {
      return null
    }

    val cachedPhotoInfoList = photoAdditionalInfoRepository.findMany(photoNameList)
    if (!netUtils.canAccessNetwork()) {
      //if there is no wifi and we can't access network without wifi -
      //use whatever there is in the cache
      return cachedPhotoInfoList
    }

    if (cachedPhotoInfoList.size == photoNameList.size) {
      Timber.tag(TAG).d("Found enough in the database")
      return cachedPhotoInfoList
    }

    val notCachedList = photoAdditionalInfoRepository.findNotCached(photoNameList)
    val photoNames = notCachedList.joinToString(separator = Constants.DELIMITER)

    val additionalInfoList = apiClient.getPhotosAdditionalInfo(userId, photoNames)
    if (additionalInfoList.isEmpty()) {
      Timber.tag(TAG).d("Nothing was found on the server")
      return emptyList()
    }

    if (!photoAdditionalInfoRepository.saveMany(additionalInfoList)) {
      throw DatabaseException("Could not cache additional photo info in the database")
    }

    return PhotoAdditionalInfoMapper.FromResponse.toPhotoAdditionalInfoList(additionalInfoList)
  }

}