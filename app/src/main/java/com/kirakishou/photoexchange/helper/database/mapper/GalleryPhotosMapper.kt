package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoEntity
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhotoInfo
import net.response.GalleryPhotosResponse

object GalleryPhotosMapper {

  object FromEntity {
    fun toGalleryPhoto(galleryPhotoEntity: GalleryPhotoEntity): GalleryPhoto {
      return GalleryPhoto(
        galleryPhotoEntity.photoName,
        galleryPhotoEntity.lon,
        galleryPhotoEntity.lat,
        galleryPhotoEntity.uploadedOn,
        galleryPhotoEntity.favouritedCount,
        GalleryPhotoInfo.empty(galleryPhotoEntity.photoName)
      )
    }

    fun toGalleryPhotos(galleryPhotoEntityList: List<GalleryPhotoEntity>): List<GalleryPhoto> {
      return galleryPhotoEntityList.map { toGalleryPhoto(it) }
    }
  }

  object FromObject {
    fun toGalleryPhotoEntity(time: Long, galleryPhoto: GalleryPhoto): GalleryPhotoEntity {
      return GalleryPhotoEntity.create(
        galleryPhoto.photoName,
        galleryPhoto.lon,
        galleryPhoto.lat,
        galleryPhoto.uploadedOn,
        galleryPhoto.favouritesCount,
        time
      )
    }

    fun toGalleryPhotoEntities(time: Long, galleryPhotoList: List<GalleryPhoto>): List<GalleryPhotoEntity> {
      return galleryPhotoList.map { toGalleryPhotoEntity(time, it) }
    }
  }

  object FromResponse {
    object ToObject {
      fun toGalleryPhoto(galleryPhotoResponseData: GalleryPhotosResponse.GalleryPhotoResponseData): GalleryPhoto {
        return GalleryPhoto(
          galleryPhotoResponseData.photoName,
          galleryPhotoResponseData.lon,
          galleryPhotoResponseData.lat,
          galleryPhotoResponseData.uploadedOn,
          galleryPhotoResponseData.favouritesCount,
          GalleryPhotoInfo.empty(galleryPhotoResponseData.photoName)
        )
      }

      fun toGalleryPhotoList(galleryPhotoResponseDataList: List<GalleryPhotosResponse.GalleryPhotoResponseData>): List<GalleryPhoto> {
        return galleryPhotoResponseDataList.map { toGalleryPhoto(it) }
      }
    }

    object ToEntity {
      fun toGalleryPhotoEntity(time: Long, galleryPhotoResponseData: GalleryPhotosResponse.GalleryPhotoResponseData): GalleryPhotoEntity {
        return GalleryPhotoEntity.create(
          galleryPhotoResponseData.photoName,
          galleryPhotoResponseData.lon,
          galleryPhotoResponseData.lat,
          galleryPhotoResponseData.uploadedOn,
          galleryPhotoResponseData.favouritesCount,
          time
        )
      }


      fun toGalleryPhotoEntitiesList(time: Long, galleryPhotoResponseDataList: List<GalleryPhotosResponse.GalleryPhotoResponseData>): List<GalleryPhotoEntity> {
        return galleryPhotoResponseDataList.map { toGalleryPhotoEntity(time, it) }
      }
    }
  }
}