package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosInfoMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhotoInfo
import net.response.GalleryPhotoInfoResponse

open class GalleryPhotoInfoLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils
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

  open fun find(photoName: String): GalleryPhotoInfoEntity? {
    return galleryPhotoInfoDao.find(photoName)
  }

  open fun findMany(photoNameList: List<String>): List<GalleryPhotoInfo> {
    return GalleryPhotosInfoMapper.ToObject.toGalleryPhotoInfoList(galleryPhotoInfoDao.findMany(photoNameList))
  }
}