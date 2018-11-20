package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosInfoMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
import net.response.GalleryPhotoInfoResponse

open class GalleryPhotoInfoLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val galleryPhotoInfoCacheMaxLiveTime: Long
) {
  private val TAG = "GalleryPhotoInfoLocalSource"
  private val galleryPhotoInfoDao = database.galleryPhotoInfoDao()


  fun save(galleryPhotoInfoEntity: GalleryPhotoInfoEntity): Boolean {
    return galleryPhotoInfoDao.save(galleryPhotoInfoEntity).isSuccess()
  }

  open fun saveMany(
    galleryPhotoInfoList: List<GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData>
  ): Boolean {
    val now = timeUtils.getTimeFast()
    val galleryPhotoInfoEntityList = GalleryPhotosInfoMapper.FromResponseData.ToEntity
      .toGalleryPhotoInfoEntityList(now, galleryPhotoInfoList)

    return galleryPhotoInfoDao.saveMany(galleryPhotoInfoEntityList).size == galleryPhotoInfoList.size
  }

  open fun findMany(
    galleryPhotoIds: List<Long>
  ): List<GalleryPhotoInfo> {
    return GalleryPhotosInfoMapper.ToObject.toGalleryPhotoInfoList(galleryPhotoInfoDao.findMany(galleryPhotoIds))
  }

  open fun findById(galleryPhotoId: Long): GalleryPhotoInfoEntity? {
    return galleryPhotoInfoDao.find(galleryPhotoId)
  }

  fun deleteOld() {
    val now = timeUtils.getTimeFast()
    galleryPhotoInfoDao.deleteOlderThan(now - galleryPhotoInfoCacheMaxLiveTime)
  }
}