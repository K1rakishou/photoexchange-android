package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoEntity
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse

object GalleryPhotoMapper {

    fun toGalleryPhoto(galleryPhotoEntity: GalleryPhotoEntity): GalleryPhoto {
        return GalleryPhoto(
            galleryPhotoEntity.galleryPhotoId,
            galleryPhotoEntity.photoName,
            galleryPhotoEntity.lon,
            galleryPhotoEntity.lat,
            galleryPhotoEntity.uploadedOn,
            galleryPhotoEntity.favouritedCount,
            galleryPhotoEntity.isFavourited,
            galleryPhotoEntity.isReported
        )
    }

    fun toGalleryPhotos(galleryPhotoEntityList: List<GalleryPhotoEntity>): List<GalleryPhoto> {
        return galleryPhotoEntityList.map { toGalleryPhoto(it) }
    }

    fun toGalleryPhotoEntity(galleryPhoto: GalleryPhoto): GalleryPhotoEntity {
        return GalleryPhotoEntity.create(
            galleryPhoto.galleryPhotoId,
            galleryPhoto.photoName,
            galleryPhoto.lon,
            galleryPhoto.lat,
            galleryPhoto.uploadedOn,
            galleryPhoto.favouritesCount,
            galleryPhoto.isFavourited,
            galleryPhoto.isReported
        )
    }

    fun toGalleryPhotoEntities(galleryPhotoList: List<GalleryPhoto>): List<GalleryPhotoEntity> {
        return galleryPhotoList.map { toGalleryPhotoEntity(it) }
    }

    fun toGalleryPhoto(galleryPhotoResponseData: GalleryPhotosResponse.GalleryPhotoResponseData): GalleryPhoto {
        return GalleryPhoto(
            galleryPhotoResponseData.id,
            galleryPhotoResponseData.photoName,
            galleryPhotoResponseData.lon,
            galleryPhotoResponseData.lat,
            galleryPhotoResponseData.uploadedOn,
            galleryPhotoResponseData.favouritesCount,
            galleryPhotoResponseData.isFavourited,
            galleryPhotoResponseData.isReported
        )
    }

    fun toGalleryPhotoList(galleryPhotoResponseDataList: List<GalleryPhotosResponse.GalleryPhotoResponseData>): List<GalleryPhoto> {
        return galleryPhotoResponseDataList.map { toGalleryPhoto(it) }
    }

    fun toGalleryPhotoEntity(galleryPhotoResponseData: GalleryPhotosResponse.GalleryPhotoResponseData): GalleryPhotoEntity {
        return GalleryPhotoEntity.create(
            galleryPhotoResponseData.id,
            galleryPhotoResponseData.photoName,
            galleryPhotoResponseData.lon,
            galleryPhotoResponseData.lat,
            galleryPhotoResponseData.uploadedOn,
            galleryPhotoResponseData.favouritesCount,
            galleryPhotoResponseData.isFavourited,
            galleryPhotoResponseData.isReported
        )
    }


    fun toGalleryPhotoEntitiesList(galleryPhotoResponseDataList: List<GalleryPhotosResponse.GalleryPhotoResponseData>): List<GalleryPhotoEntity> {
        return galleryPhotoResponseDataList.map { toGalleryPhotoEntity(it) }
    }
}