package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
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
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GalleryPhotoRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val galleryPhotoCacheMaxLiveTime: Long,
  private val galleryPhotoInfoCacheMaxLiveTime: Long,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "GalleryPhotoRepository"
  private val galleryPhotoDao = database.galleryPhotoDao()
  private val galleryPhotoInfoDao = database.galleryPhotoInfoDao()

  open suspend fun saveManyInfo(galleryPhotoInfoList: List<GalleryPhotoInfoResponse.GalleryPhotosInfoData>): Boolean {
    return withContext(coroutineContext) {
      val now = timeUtils.getTimeFast()
      val galleryPhotoInfoEntityList = GalleryPhotosInfoMapper.FromResponse.ToEntity
        .toGalleryPhotoInfoEntityList(now, galleryPhotoInfoList)

      return@withContext galleryPhotoInfoDao.saveMany(galleryPhotoInfoEntityList).size == galleryPhotoInfoList.size
    }
  }

  open suspend fun saveMany(galleryPhotos: List<GalleryPhotosResponse.GalleryPhotoResponseData>): Boolean {
    return withContext(coroutineContext) {
      val now = timeUtils.getTimeFast()
      return@withContext galleryPhotoDao.saveMany(GalleryPhotosMapper.FromResponse.ToEntity
        .toGalleryPhotoEntitiesList(now, galleryPhotos)).size == galleryPhotos.size
    }
  }

  suspend fun getPageOfGalleryPhotos(time: Long, count: Int): List<GalleryPhoto> {
    return withContext(coroutineContext) {
      return@withContext GalleryPhotosMapper.FromEntity.toGalleryPhotos(galleryPhotoDao.getPage(time, count))
    }
  }

  suspend fun findManyInfo(galleryPhotoIds: List<Long>): List<GalleryPhotoInfo> {
    return withContext(coroutineContext) {
      return@withContext GalleryPhotosInfoMapper.ToObject.toGalleryPhotoInfoList(galleryPhotoInfoDao.findMany(galleryPhotoIds))
    }
  }

  suspend fun findByPhotoName(photoName: String): GalleryPhoto? {
    return withContext(coroutineContext) {
      val galleryPhotoEntity = galleryPhotoDao.findByPhotoName(photoName)
      if (galleryPhotoEntity == null) {
        return@withContext null
      }

      val galleryPhotoInfoEntity = galleryPhotoInfoDao.find(galleryPhotoEntity.galleryPhotoId)
      val galleryPhoto = GalleryPhotosMapper.FromEntity.toGalleryPhoto(galleryPhotoEntity)
      galleryPhoto.galleryPhotoInfo = GalleryPhotosInfoMapper.ToObject.toGalleryPhotoInfo(galleryPhotoInfoEntity)

      return@withContext galleryPhoto
    }
  }

  open suspend fun findMany(galleryPhotoIds: List<Long>): List<GalleryPhoto> {
    return withContext(coroutineContext) {
      val galleryPhotos = GalleryPhotosMapper.FromEntity.toGalleryPhotos(galleryPhotoDao.findMany(galleryPhotoIds))

      for (galleryPhoto in galleryPhotos) {
        val galleryPhotoInfo = galleryPhotoInfoDao.find(galleryPhoto.galleryPhotoId)
        galleryPhoto.galleryPhotoInfo = GalleryPhotosInfoMapper.ToObject.toGalleryPhotoInfo(galleryPhotoInfo)
      }

      return@withContext galleryPhotos
    }
  }

  suspend fun findAllPhotosTest(): List<GalleryPhotoEntity> {
    return withContext(coroutineContext) {
      return@withContext galleryPhotoDao.findAll()
    }
  }

  suspend fun findAllPhotosInfoTest(): List<GalleryPhotoInfoEntity> {
    return withContext(coroutineContext) {
      return@withContext galleryPhotoInfoDao.findAll()
    }
  }

  suspend fun updateFavouritesCount(photoName: String, favouritesCount: Long): Boolean {
    return withContext(coroutineContext) {
      return@withContext galleryPhotoDao.updateFavouritesCount(photoName, favouritesCount).isSuccess()
    }
  }

  suspend fun favouritePhoto(galleryPhotoId: Long): Boolean {
    return withContext(coroutineContext) {
      var galleryPhotoInfoEntity = galleryPhotoInfoDao.find(galleryPhotoId)
      if (galleryPhotoInfoEntity == null) {
        galleryPhotoInfoEntity = GalleryPhotoInfoEntity.create(galleryPhotoId, true, false, timeUtils.getTimeFast())
      } else {
        galleryPhotoInfoEntity.isFavourited = !galleryPhotoInfoEntity.isFavourited
      }

      return@withContext galleryPhotoInfoDao.save(galleryPhotoInfoEntity).isSuccess()
    }
  }

  suspend fun reportPhoto(photoId: Long): Boolean {
    return withContext(coroutineContext) {
      var galleryPhotoInfoEntity = galleryPhotoInfoDao.find(photoId)
      if (galleryPhotoInfoEntity == null) {
        galleryPhotoInfoEntity = GalleryPhotoInfoEntity.create(photoId, false, true, timeUtils.getTimeFast())
      } else {
        galleryPhotoInfoEntity.isReported = !galleryPhotoInfoEntity.isReported
      }

      return@withContext galleryPhotoInfoDao.save(galleryPhotoInfoEntity).isSuccess()
    }
  }

  open suspend fun deleteOldPhotos() {
    withContext(coroutineContext) {
      val oldCount = findAllPhotosTest().size
      val now = timeUtils.getTimeFast()
      galleryPhotoDao.deleteOlderThan(now - galleryPhotoCacheMaxLiveTime)

      val newCount = findAllPhotosTest().size
      if (newCount < oldCount) {
        Timber.tag(TAG).d("Deleted ${newCount - oldCount} galleryPhotos from the cache")
      }
    }
  }

  suspend fun deleteOldPhotosInfo() {
    return withContext(coroutineContext) {
      val oldCount = findAllPhotosInfoTest().size
      val now = timeUtils.getTimeFast()
      galleryPhotoInfoDao.deleteOlderThan(now - galleryPhotoInfoCacheMaxLiveTime)

      val newCount = findAllPhotosInfoTest().size
      if (newCount < oldCount) {
        Timber.tag(TAG).d("Deleted ${newCount - oldCount} galleryPhotosInfo from the cache")
      }
    }
  }
}