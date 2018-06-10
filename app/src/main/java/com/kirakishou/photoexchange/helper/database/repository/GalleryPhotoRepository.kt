package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosInfoMapper
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotoInfoResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse

class GalleryPhotoRepository(
    private val database: MyDatabase,
    private val timeUtils: TimeUtils
) {
    private val galleryPhotoDao = database.galleryPhotoDao()
    private val galleryPhotoInfoDao = database.galleryPhotoInfoDao()

    fun saveManyInfo(galleryPhotoInfoList: List<GalleryPhotoInfoResponse.GalleryPhotosInfoData>): Boolean {
        val galleryPhotoInfoEntityList = GalleryPhotosInfoMapper.FromResponse.ToEntity
            .toGalleryPhotoInfoEntityList(timeUtils.getTimeFast(), galleryPhotoInfoList)

        return galleryPhotoInfoDao.saveMany(galleryPhotoInfoEntityList).size == galleryPhotoInfoList.size
    }

    fun saveMany(galleryPhotos: List<GalleryPhotosResponse.GalleryPhotoResponseData>): Boolean {
        val now = timeUtils.getTimeFast()
        return galleryPhotoDao.saveMany(GalleryPhotosMapper.FromResponse.ToEntity
            .toGalleryPhotoEntitiesList(now, galleryPhotos)).size == galleryPhotos.size
    }

    fun findManyInfo(galleryPhotoIds: List<Long>, timeDelta: Long): List<GalleryPhotoInfo> {
        val time = timeUtils.getTimeFast() - timeDelta
        return GalleryPhotosInfoMapper.ToObject.toGalleryPhotoInfoList(galleryPhotoInfoDao.findMany(galleryPhotoIds, time))
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

    fun findMany(galleryPhotoIds: List<Long>): List<GalleryPhoto> {
        return GalleryPhotosMapper.FromEntity.toGalleryPhotos(galleryPhotoDao.findMany(galleryPhotoIds))
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

}