package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoEntity
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.photo.PhotoAdditionalInfo
import net.response.data.GalleryPhotoResponseData

object GalleryPhotosMapper {

  object FromEntity {
    fun toGalleryPhoto(galleryPhotoEntity: GalleryPhotoEntity): GalleryPhoto {
      return GalleryPhoto(
        galleryPhotoEntity.photoName,
        LonLat(
          galleryPhotoEntity.lon,
          galleryPhotoEntity.lat
        ),
        galleryPhotoEntity.uploadedOn,
        PhotoAdditionalInfo.empty(galleryPhotoEntity.photoName)
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
        galleryPhoto.lonLat.lon,
        galleryPhoto.lonLat.lat,
        galleryPhoto.uploadedOn,
        time
      )
    }

    fun toGalleryPhotoEntities(time: Long, galleryPhotoList: List<GalleryPhoto>): List<GalleryPhotoEntity> {
      return galleryPhotoList.map { toGalleryPhotoEntity(time, it) }
    }
  }

  object FromResponse {
    object ToObject {
      fun toGalleryPhoto(galleryPhotoResponseData: GalleryPhotoResponseData): GalleryPhoto {
        return GalleryPhoto(
          galleryPhotoResponseData.photoName,
          LonLat(
            galleryPhotoResponseData.lon,
            galleryPhotoResponseData.lat
          ),
          galleryPhotoResponseData.uploadedOn,
          PhotoAdditionalInfo.empty(galleryPhotoResponseData.photoName)
        )
      }

      fun toGalleryPhotoList(galleryPhotoResponseDataList: List<GalleryPhotoResponseData>): List<GalleryPhoto> {
        return galleryPhotoResponseDataList.map { toGalleryPhoto(it) }
      }
    }

    object ToEntity {
      fun toGalleryPhotoEntity(time: Long, galleryPhotoResponseData: GalleryPhotoResponseData): GalleryPhotoEntity {
        return GalleryPhotoEntity.create(
          galleryPhotoResponseData.photoName,
          galleryPhotoResponseData.lon,
          galleryPhotoResponseData.lat,
          galleryPhotoResponseData.uploadedOn,
          time
        )
      }

      fun toGalleryPhotoEntitiesList(time: Long, galleryPhotoResponseDataList: List<GalleryPhotoResponseData>): List<GalleryPhotoEntity> {
        return galleryPhotoResponseDataList.map { toGalleryPhotoEntity(time, it) }
      }
    }
  }
}