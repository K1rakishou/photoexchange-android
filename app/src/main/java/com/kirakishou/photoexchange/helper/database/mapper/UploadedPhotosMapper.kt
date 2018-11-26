package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import net.response.GetUploadedPhotosResponse

object UploadedPhotosMapper {

  object FromResponse {
    object ToEntity {
      fun toUploadedPhotoEntity(time: Long, uploadedPhotoData: GetUploadedPhotosResponse.UploadedPhotoResponseData): UploadedPhotoEntity {
        return UploadedPhotoEntity.create(
          uploadedPhotoData.photoName,
          uploadedPhotoData.photoId,
          uploadedPhotoData.uploaderLon,
          uploadedPhotoData.uploaderLat,
          uploadedPhotoData.receiverInfoResponseData?.receiverLon,
          uploadedPhotoData.receiverInfoResponseData?.receiverLat,
          uploadedPhotoData.uploadedOn,
          time
        )
      }

      fun toUploadedPhotoEntities(time: Long, uploadedPhotoDataList: List<GetUploadedPhotosResponse.UploadedPhotoResponseData>): List<UploadedPhotoEntity> {
        return uploadedPhotoDataList.map { toUploadedPhotoEntity(time, it) }
      }
    }

    object ToObject {
      fun toUploadedPhoto(uploadedPhotoData: GetUploadedPhotosResponse.UploadedPhotoResponseData): UploadedPhoto {
        return UploadedPhoto(
          uploadedPhotoData.photoId,
          uploadedPhotoData.photoName,
          uploadedPhotoData.uploaderLon,
          uploadedPhotoData.uploaderLat,
          null,
          uploadedPhotoData.uploadedOn
        )
      }

      fun toUploadedPhotos(uploadedPhotoDataList: List<GetUploadedPhotosResponse.UploadedPhotoResponseData>): List<UploadedPhoto> {
        return uploadedPhotoDataList.map { toUploadedPhoto(it) }
      }
    }
  }

  object FromEntity {
    object ToObject {
      fun toUploadedPhoto(uploadedPhotoEntity: UploadedPhotoEntity): UploadedPhoto {
        return UploadedPhoto(
          uploadedPhotoEntity.photoId!!,
          uploadedPhotoEntity.photoName,
          uploadedPhotoEntity.uploaderLon,
          uploadedPhotoEntity.uploaderLat,
          null,
          uploadedPhotoEntity.uploadedOn!!
        )
      }

      fun toUploadedPhotos(uploadedPhotoEntityList: List<UploadedPhotoEntity>): List<UploadedPhoto> {
        return uploadedPhotoEntityList.map { toUploadedPhoto(it) }
      }
    }
  }

  object FromObject {
    object ToEntity {
      fun toUploadedPhotoEntity(photoId: Long, photoName: String, lon: Double, lat: Double, time: Long, uploadedOn: Long): UploadedPhotoEntity {
        return UploadedPhotoEntity.createWithoutReceiverInfo(
          photoName,
          photoId,
          lon,
          lat,
          uploadedOn,
          time
        )
      }
    }
  }
}