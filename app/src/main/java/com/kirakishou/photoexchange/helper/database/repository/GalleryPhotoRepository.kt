package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotoMapper
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse

class GalleryPhotoRepository(
    private val database: MyDatabase
) {
    private val galleryPhotoDao = database.galleryPhotoDao()

    fun saveMany(galleryPhotos: List<GalleryPhotosResponse.GalleryPhotoResponseData>): Boolean {
        return galleryPhotoDao.saveMany(GalleryPhotoMapper.toGalleryPhotoEntitiesList(galleryPhotos)).size == galleryPhotos.size
    }

    fun findMany(galleryPhotoIds: List<Long>): List<GalleryPhoto> {
        return GalleryPhotoMapper.toGalleryPhotos(galleryPhotoDao.findMany(galleryPhotoIds))
    }
}