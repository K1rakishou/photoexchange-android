package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.PhotoAdditionalInfoMapper
import com.kirakishou.photoexchange.helper.database.repository.PhotoAdditionalInfoRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.mvp.model.photo.PhotoAdditionalInfo
import kotlinx.coroutines.withContext
import timber.log.Timber

class GetPhotoAdditionalInfoUseCase(
  private val apiClient: ApiClient,
  private val netUtils: NetUtils,
  private val photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  private val TAG = "PhotoAdditionalInfoUtilsImpl"

  suspend fun <T> appendAdditionalPhotoInfo(
    galleryPhotos: List<T>,
    photoNameSelectorFunc: (T) -> String,
    copyFunc: (T, PhotoAdditionalInfo) -> T
  ): List<T> {
    return withContext(coroutineContext) {
      val userId = settingsRepository.getUserId()

      val additionalPhotoInfoList = getPhotoAdditionalInfos(
        userId,
        galleryPhotos.map { photoNameSelectorFunc(it) }
      )

      if (additionalPhotoInfoList == null) {
        return@withContext galleryPhotos
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

      return@withContext resultList
    }
  }

  //returns null when userId is empty
  suspend fun getPhotoAdditionalInfoByPhotoNameList(
    photoNameList: List<String>
  ): List<PhotoAdditionalInfo>? {
    return withContext(coroutineContext) {
      val userId = settingsRepository.getUserId()

      if (userId.isEmpty()) {
        return@withContext null
      }

      val result = getPhotoAdditionalInfos(userId, photoNameList)
      if (result == null) {
        return@withContext null
      }

      return@withContext result
    }
  }

  private suspend fun getPhotoAdditionalInfos(
    userId: String,
    photoNameList: List<String>
  ): List<PhotoAdditionalInfo>? {
    photoAdditionalInfoRepository.deleteOld()

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