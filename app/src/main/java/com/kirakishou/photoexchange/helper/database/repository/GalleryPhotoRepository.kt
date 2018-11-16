package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosInfoMapper
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotoInfoResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse
import timber.log.Timber

open class GalleryPhotoRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val galleryPhotoCacheMaxLiveTime: Long,
  private val galleryPhotoInfoCacheMaxLiveTime: Long
) {
  private val TAG = "GalleryPhotoRepository"
  private val galleryPhotoDao = database.galleryPhotoDao()
  private val galleryPhotoInfoDao = database.galleryPhotoInfoDao()

  open fun saveManyInfo(galleryPhotoInfoList: List<GalleryPhotoInfoResponse.GalleryPhotosInfoData>): Boolean {
    val now = timeUtils.getTimeFast()
    val galleryPhotoInfoEntityList = GalleryPhotosInfoMapper.FromResponse.ToEntity
      .toGalleryPhotoInfoEntityList(now, galleryPhotoInfoList)

    return galleryPhotoInfoDao.saveMany(galleryPhotoInfoEntityList).size == galleryPhotoInfoList.size
  }

  open fun saveMany(galleryPhotos: List<GalleryPhotosResponse.GalleryPhotoResponseData>): Boolean {
    val now = timeUtils.getTimeFast()
    return galleryPhotoDao.saveMany(GalleryPhotosMapper.FromResponse.ToEntity
      .toGalleryPhotoEntitiesList(now, galleryPhotos)).size == galleryPhotos.size
  }

  fun getPageOfGalleryPhotos(time: Long, count: Int): List<GalleryPhoto> {
    return GalleryPhotosMapper.FromEntity.toGalleryPhotos(galleryPhotoDao.getPage(time, count))
  }

  fun findManyInfo(galleryPhotoIds: List<Long>): List<GalleryPhotoInfo> {
    return GalleryPhotosInfoMapper.ToObject.toGalleryPhotoInfoList(galleryPhotoInfoDao.findMany(galleryPhotoIds))
  }

  fun findByPhotoName(photoName: String): GalleryPhoto? {
    val galleryPhotoEntity = galleryPhotoDao.findByPhotoName(photoName)
    if (galleryPhotoEntity == null) {
      return null
    }

    val galleryPhotoInfoEntity = galleryPhotoInfoDao.find(galleryPhotoEntity.galleryPhotoId)
    val galleryPhoto = GalleryPhotosMapper.FromEntity.toGalleryPhoto(galleryPhotoEntity)
    galleryPhoto.galleryPhotoInfo = GalleryPhotosInfoMapper.ToObject.toGalleryPhotoInfo(galleryPhotoInfoEntity)

    return galleryPhoto
  }

  open fun findMany(galleryPhotoIds: List<Long>): List<GalleryPhoto> {
    val galleryPhotos = GalleryPhotosMapper.FromEntity.toGalleryPhotos(galleryPhotoDao.findMany(galleryPhotoIds))

    for (galleryPhoto in galleryPhotos) {
      val galleryPhotoInfo = galleryPhotoInfoDao.find(galleryPhoto.galleryPhotoId)
      galleryPhoto.galleryPhotoInfo = GalleryPhotosInfoMapper.ToObject.toGalleryPhotoInfo(galleryPhotoInfo)
    }

    return galleryPhotos
  }

  fun findAllPhotosTest(): List<GalleryPhotoEntity> {
    return galleryPhotoDao.findAll()
  }

  fun findAllPhotosInfoTest(): List<GalleryPhotoInfoEntity> {
    return galleryPhotoInfoDao.findAll()
  }

  fun updateFavouritesCount(photoName: String, favouritesCount: Long): Boolean {
    return galleryPhotoDao.updateFavouritesCount(photoName, favouritesCount).isSuccess()
  }

  fun favouritePhoto(galleryPhotoId: Long): Boolean {
    var galleryPhotoInfoEntity = galleryPhotoInfoDao.find(galleryPhotoId)
    if (galleryPhotoInfoEntity == null) {
      galleryPhotoInfoEntity = GalleryPhotoInfoEntity.create(galleryPhotoId, true, false, timeUtils.getTimeFast())
    } else {
      galleryPhotoInfoEntity.isFavourited = !galleryPhotoInfoEntity.isFavourited
    }

    return galleryPhotoInfoDao.save(galleryPhotoInfoEntity).isSuccess()
  }

  fun reportPhoto(photoId: Long): Boolean {
    var galleryPhotoInfoEntity = galleryPhotoInfoDao.find(photoId)
    if (galleryPhotoInfoEntity == null) {
      galleryPhotoInfoEntity = GalleryPhotoInfoEntity.create(photoId, false, true, timeUtils.getTimeFast())
    } else {
      galleryPhotoInfoEntity.isReported = !galleryPhotoInfoEntity.isReported
    }

    return galleryPhotoInfoDao.save(galleryPhotoInfoEntity).isSuccess()
  }

  open fun deleteOldPhotos() {
    val oldCount = findAllPhotosTest().size
    val now = timeUtils.getTimeFast()
    galleryPhotoDao.deleteOlderThan(now - galleryPhotoCacheMaxLiveTime)

    val newCount = findAllPhotosTest().size
    if (newCount < oldCount) {
      Timber.tag(TAG).d("Deleted ${newCount - oldCount} galleryPhotos from the cache")
    }
  }

  fun deleteOldPhotosInfo() {
    val oldCount = findAllPhotosInfoTest().size
    val now = timeUtils.getTimeFast()
    galleryPhotoInfoDao.deleteOlderThan(now - galleryPhotoInfoCacheMaxLiveTime)

    val newCount = findAllPhotosInfoTest().size
    if (newCount < oldCount) {
      Timber.tag(TAG).d("Deleted ${newCount - oldCount} galleryPhotosInfo from the cache")
    }
  }
}