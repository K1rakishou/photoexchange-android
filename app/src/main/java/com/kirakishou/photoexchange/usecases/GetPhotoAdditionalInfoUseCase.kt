package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.PhotoAdditionalInfoMapper
import com.kirakishou.photoexchange.helper.database.repository.PhotoAdditionalInfoRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.mvrx.model.photo.PhotoAdditionalInfo
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GetPhotoAdditionalInfoUseCase(
  private val apiClient: ApiClient,
  private val netUtils: NetUtils,
  private val photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetPhotoAdditionalInfoUseCase"

  //FIXME: sometime this method does not send the name of the last photo in the list thus losing photoadditionalinfo
  suspend fun <T> appendAdditionalPhotoInfo(
    galleryPhotos: List<T>,
    photoNameSelectorFunc: (T) -> String,
    copyFunc: (T, PhotoAdditionalInfo) -> T
  ): List<T> {
    if (galleryPhotos.isEmpty()) {
      return emptyList()
    }

    return withContext(coroutineContext) {
      val userUuid = settingsRepository.getUserUuid()
      if (userUuid.isEmpty()) {
        return@withContext galleryPhotos
      }

      val additionalPhotoInfoList = getPhotoAdditionalInfos(
        userUuid,
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
          .copy(hasUserUuid = userUuid.isNotEmpty())

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
      val userUuid = settingsRepository.getUserUuid()
      if (userUuid.isEmpty()) {
        return@withContext null
      }

      return@withContext try {
        getPhotoAdditionalInfos(userUuid, photoNameList)
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error)
        null
      }
    }
  }

  //returns null when userId is empty
  private suspend fun getPhotoAdditionalInfos(
    userUuid: String,
    photoNameList: List<String>
  ): List<PhotoAdditionalInfo>? {
    if (userUuid.isEmpty()) {
      return null
    }

    photoAdditionalInfoRepository.deleteOld()

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

    val additionalInfoList = apiClient.getPhotosAdditionalInfo(userUuid, photoNames)
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