package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.PhotoAdditionalInfoMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.PhotoAdditionalInfo
import net.response.data.PhotoAdditionalInfoResponseData
import timber.log.Timber

open class PhotoAdditionalInfoLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val insertedEarlierThanTimeDelta: Long
) {
  private val TAG = "PhotoAdditionalInfoLocalSource"
  private val photoAdditionalInfoDao = database.photoAdditionalInfoDao()

  open fun save(photoAdditionalInfo: PhotoAdditionalInfo): Boolean {
    val photoAdditionalInfoEntity = PhotoAdditionalInfoMapper.ToEntity.toPhotoAdditionalInfoEntity(
      timeUtils.getTimeFast(),
      photoAdditionalInfo
    )

    return photoAdditionalInfoDao.save(photoAdditionalInfoEntity).isSuccess()
  }

  open fun saveMany(additionalInfoResponseDataList: List<PhotoAdditionalInfoResponseData>): Boolean {
    val additionalInfoList = PhotoAdditionalInfoMapper.FromResponse.ToEntity.toEntities(
      timeUtils.getTimeFast(),
      additionalInfoResponseDataList
    )

    return photoAdditionalInfoDao.saveMany(additionalInfoList).size == additionalInfoResponseDataList.size
  }

  open fun findByPhotoName(photoName: String): PhotoAdditionalInfo? {
    val photoAdditionalInfo = photoAdditionalInfoDao.find(photoName)
    if (photoAdditionalInfo == null) {
      return null
    }

    return PhotoAdditionalInfoMapper.FromEntity.toPhotoAdditionalInfo(photoAdditionalInfo)
  }

  open fun findMany(photoNameList: List<String>): List<PhotoAdditionalInfo> {
    val additionalInfoEntityList = photoAdditionalInfoDao.findMany(photoNameList)

    return PhotoAdditionalInfoMapper.FromEntity.toPhotoAdditionalInfoList(
      additionalInfoEntityList
    )
  }

  open fun findNotCached(photoNameList: List<String>): List<String> {
    val alreadyCached = photoAdditionalInfoDao.findMany(photoNameList)
      .map { it.photoName }
      .toSet()

    return photoNameList.filter { photoName -> photoName !in alreadyCached }
  }

  open fun updateFavouritesCount(photoName: String, favouritesCount: Long): Boolean {
    return photoAdditionalInfoDao.updateFavouritesCount(photoName, favouritesCount) == 1
  }

  open fun deleteOld() {
    val now = timeUtils.getTimeFast()
    val deletedCount = photoAdditionalInfoDao.deleteOlderThan(now - insertedEarlierThanTimeDelta)

    Timber.tag(TAG).d("deleted $deletedCount gallery photos")
  }
}