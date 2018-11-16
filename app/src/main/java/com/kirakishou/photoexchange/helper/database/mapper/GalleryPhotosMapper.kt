package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoEntity
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse
import java.util.concurrent.TimeUnit

object GalleryPhotosMapper {

  object FromEntity {
    fun toGalleryPhoto(galleryPhotoEntity: GalleryPhotoEntity): GalleryPhoto {
      return GalleryPhoto(
        galleryPhotoEntity.galleryPhotoId,
        galleryPhotoEntity.photoName,
        galleryPhotoEntity.lon,
        galleryPhotoEntity.lat,
        galleryPhotoEntity.uploadedOn,
        galleryPhotoEntity.favouritedCount
      )
    }

    fun toGalleryPhotos(galleryPhotoEntityList: List<GalleryPhotoEntity>): List<GalleryPhoto> {
      return galleryPhotoEntityList.map { toGalleryPhoto(it) }
    }
  }

  object FromObject {
    fun toGalleryPhotoEntity(time: Long, galleryPhoto: GalleryPhoto): GalleryPhotoEntity {
      return GalleryPhotoEntity.create(
        galleryPhoto.galleryPhotoId,
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
          galleryPhotoResponseData.id,
          galleryPhotoResponseData.photoName,
          galleryPhotoResponseData.lon,
          galleryPhotoResponseData.lat,
          galleryPhotoResponseData.uploadedOn,
          galleryPhotoResponseData.favouritesCount
        )
      }

      fun toGalleryPhotoList(galleryPhotoResponseDataList: List<GalleryPhotosResponse.GalleryPhotoResponseData>): List<GalleryPhoto> {
        return galleryPhotoResponseDataList.map { toGalleryPhoto(it) }
      }
    }

    object ToEntity {
      fun toGalleryPhotoEntity(time: Long, galleryPhotoResponseData: GalleryPhotosResponse.GalleryPhotoResponseData): GalleryPhotoEntity {
        return GalleryPhotoEntity.create(
          galleryPhotoResponseData.id,
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