package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotoInfoMapper
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotoMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotoInfoResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse

class GalleryPhotoRepository(
    private val database: MyDatabase
) {
    private val galleryPhotoDao = database.galleryPhotoDao()
    private val galleryPhotoInfoDao = database.galleryPhotoInfoDao()

    fun saveManyInfo(galleryPhotoInfoList: List<GalleryPhotoInfoResponse.GalleryPhotosInfoData>): Boolean {
        val galleryPhotoInfoEntityList = GalleryPhotoInfoMapper.FromResponse.ToEntity.toGalleryPhotoInfoEntityList(galleryPhotoInfoList)
        return galleryPhotoInfoDao.saveMany(galleryPhotoInfoEntityList).size == galleryPhotoInfoList.size
    }

    fun saveMany(galleryPhotos: List<GalleryPhotosResponse.GalleryPhotoResponseData>): Boolean {
        return galleryPhotoDao.saveMany(GalleryPhotoMapper.FromResponse.ToEntity.toGalleryPhotoEntitiesList(galleryPhotos)).size == galleryPhotos.size
    }

    fun findManyInfo(galleryPhotoIds: List<Long>, timeDelta: Long): List<GalleryPhotoInfo> {
        val time = TimeUtils.getTimeFast() - timeDelta
        return GalleryPhotoInfoMapper.ToObject.toGalleryPhotoInfoList(galleryPhotoInfoDao.findMany(galleryPhotoIds, time))
    }

    fun findByPhotoName(photoName: String): GalleryPhoto? {
        val galleryPhotoEntity = galleryPhotoDao.findByPhotoName(photoName)
        if (galleryPhotoEntity == null) {
            return null
        }

        val galleryPhotoInfoEntity = galleryPhotoInfoDao.find(galleryPhotoEntity.galleryPhotoId)

        val galleryPhoto = GalleryPhotoMapper.FromEntity.toGalleryPhoto(galleryPhotoEntity)
        galleryPhoto.galleryPhotoInfo = GalleryPhotoInfoMapper.ToObject.toGalleryPhotoInfo(galleryPhotoInfoEntity)

        return galleryPhoto
    }

    fun findMany(galleryPhotoIds: List<Long>): List<GalleryPhoto> {
        return GalleryPhotoMapper.FromEntity.toGalleryPhotos(galleryPhotoDao.findMany(galleryPhotoIds))
    }

    fun updateFavouritesCount(photoName: String, favouritesCount: Long): Boolean {
        return galleryPhotoDao.updateFavouritesCount(photoName, favouritesCount).isSuccess()
    }

    fun favouritePhoto(galleryPhotoId: Long): Boolean {
        var galleryPhotoInfoEntity = galleryPhotoInfoDao.find(galleryPhotoId)
        if (galleryPhotoInfoEntity == null) {
            galleryPhotoInfoEntity = GalleryPhotoInfoEntity.create(galleryPhotoId, true, false)
        } else {
            galleryPhotoInfoEntity.isFavourited = !galleryPhotoInfoEntity.isFavourited
        }

        return galleryPhotoInfoDao.save(galleryPhotoInfoEntity).isSuccess()
    }

    fun reportPhoto(photoId: Long): Boolean {
        var galleryPhotoInfoEntity = galleryPhotoInfoDao.find(photoId)
        if (galleryPhotoInfoEntity == null) {
            galleryPhotoInfoEntity = GalleryPhotoInfoEntity.create(photoId, false, true)
        } else {
            galleryPhotoInfoEntity.isReported = !galleryPhotoInfoEntity.isReported
        }

        return galleryPhotoInfoDao.save(galleryPhotoInfoEntity).isSuccess()
    }

}