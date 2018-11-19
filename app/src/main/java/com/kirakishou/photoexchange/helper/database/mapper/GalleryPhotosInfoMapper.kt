package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
import net.response.GalleryPhotoInfoResponse

object GalleryPhotosInfoMapper {

  object FromResponseData {
    object ToEntity {
      fun toGalleryPhotoInfoEntity(time: Long, galleryPhotoInfoResponseData: GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData): GalleryPhotoInfoEntity {
        return GalleryPhotoInfoEntity.create(
          galleryPhotoInfoResponseData.id,
          galleryPhotoInfoResponseData.isFavourited,
          galleryPhotoInfoResponseData.isReported,
          time
        )
      }

      fun toGalleryPhotoInfoEntityList(
        time: Long,
        galleryPhotoInfoResponseDataList: List<GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData>
      ): List<GalleryPhotoInfoEntity> {
        return galleryPhotoInfoResponseDataList.map { toGalleryPhotoInfoEntity(time, it) }
      }
    }

    object ToObject {
      fun toGalleryPhotoInfo(galleryPhotoInfoResponseData: GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData): GalleryPhotoInfo {
        return GalleryPhotoInfo(
          galleryPhotoInfoResponseData.id,
          galleryPhotoInfoResponseData.isFavourited,
          galleryPhotoInfoResponseData.isReported
        )
      }

      fun toGalleryPhotoInfoList(
        galleryPhotoInfoResponseDataList: List<GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData>
      ): List<GalleryPhotoInfo> {
        return galleryPhotoInfoResponseDataList.map { toGalleryPhotoInfo(it) }
      }
    }
  }

  object ToObject {
    fun toGalleryPhotoInfo(galleryPhotoInfoEntity: GalleryPhotoInfoEntity?): GalleryPhotoInfo? {
      if (galleryPhotoInfoEntity == null) {
        return null
      }

      return GalleryPhotoInfo(
        galleryPhotoInfoEntity.galleryPhotoId,
        galleryPhotoInfoEntity.isFavourited,
        galleryPhotoInfoEntity.isReported
      )
    }

    fun toGalleryPhotoInfoList(galleryPhotoInfoEntityList: List<GalleryPhotoInfoEntity?>): List<GalleryPhotoInfo> {
      return galleryPhotoInfoEntityList.mapNotNull { toGalleryPhotoInfo(it) }
    }
  }
}