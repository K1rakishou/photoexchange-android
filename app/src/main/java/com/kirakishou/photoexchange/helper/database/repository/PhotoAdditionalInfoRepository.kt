package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.source.local.PhotoAdditionalInfoLocalSource
import com.kirakishou.photoexchange.mvrx.model.photo.PhotoAdditionalInfo
import net.response.data.PhotoAdditionalInfoResponseData

class PhotoAdditionalInfoRepository(
  private val photoAdditionalInfoLocalSource: PhotoAdditionalInfoLocalSource
) : BaseRepository() {

  suspend fun save(photoAdditionalInfo: PhotoAdditionalInfo): Boolean {
    return photoAdditionalInfoLocalSource.save(photoAdditionalInfo)
  }

  suspend fun saveMany(additionalInfoResponseDataList: List<PhotoAdditionalInfoResponseData>): Boolean {
    return photoAdditionalInfoLocalSource.saveMany(additionalInfoResponseDataList)
  }

  suspend fun findByPhotoName(photoName: String): PhotoAdditionalInfo? {
    return photoAdditionalInfoLocalSource.findByPhotoName(photoName)
  }

  suspend fun findMany(photoNameList: List<String>): List<PhotoAdditionalInfo> {
    return photoAdditionalInfoLocalSource.findMany(photoNameList)
  }

  suspend fun findNotCached(photoNameList: List<String>): List<String> {
    return photoAdditionalInfoLocalSource.findNotCached(photoNameList)
  }

  suspend fun deleteOld() {
    photoAdditionalInfoLocalSource.deleteOld()
  }
}