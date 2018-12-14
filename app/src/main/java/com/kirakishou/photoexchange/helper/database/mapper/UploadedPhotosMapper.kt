package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import net.response.data.UploadedPhotoResponseData

object UploadedPhotosMapper {

  object FromResponse {
    object ToEntity {
      fun toUploadedPhotoEntity(time: Long, uploadedPhotoData: UploadedPhotoResponseData): UploadedPhotoEntity {
        return UploadedPhotoEntity.create(
          uploadedPhotoData.photoName,
          uploadedPhotoData.photoId,
          uploadedPhotoData.uploaderLon,
          uploadedPhotoData.uploaderLat,
          uploadedPhotoData.receiverInfoResponseData?.receiverPhotoName,
          uploadedPhotoData.receiverInfoResponseData?.receiverLon,
          uploadedPhotoData.receiverInfoResponseData?.receiverLat,
          uploadedPhotoData.uploadedOn,
          time
        )
      }

      fun toUploadedPhotoEntities(time: Long, uploadedPhotoDataList: List<UploadedPhotoResponseData>): List<UploadedPhotoEntity> {
        return uploadedPhotoDataList.map { toUploadedPhotoEntity(time, it) }
      }
    }

    object ToObject {
      fun toUploadedPhoto(uploadedPhotoData: UploadedPhotoResponseData): UploadedPhoto {
        val receiverInfo = if (uploadedPhotoData.receiverInfoResponseData == null) {
          null
        } else {
          UploadedPhoto.ReceiverInfo(
            uploadedPhotoData.receiverInfoResponseData!!.receiverPhotoName,
            LonLat(
              uploadedPhotoData.receiverInfoResponseData!!.receiverLon,
              uploadedPhotoData.receiverInfoResponseData!!.receiverLat
            )
          )
        }

        return UploadedPhoto(
          uploadedPhotoData.photoId,
          uploadedPhotoData.photoName,
          uploadedPhotoData.uploaderLon,
          uploadedPhotoData.uploaderLat,
          receiverInfo,
          uploadedPhotoData.uploadedOn
        )
      }

      fun toUploadedPhotos(uploadedPhotoDataList: List<UploadedPhotoResponseData>): List<UploadedPhoto> {
        return uploadedPhotoDataList.map { toUploadedPhoto(it) }
      }
    }
  }

  object FromEntity {
    object ToObject {
      fun toUploadedPhoto(uploadedPhotoEntity: UploadedPhotoEntity): UploadedPhoto {
        val receiverInfo = if (uploadedPhotoEntity.receiverLon == null || uploadedPhotoEntity.receiverLat == null) {
          null
        } else {
          UploadedPhoto.ReceiverInfo(
            uploadedPhotoEntity.receiverPhotoName!!,
            LonLat(
              uploadedPhotoEntity.receiverLon!!,
              uploadedPhotoEntity.receiverLat!!
            )
          )
        }

        return UploadedPhoto(
          uploadedPhotoEntity.photoId!!,
          uploadedPhotoEntity.photoName,
          uploadedPhotoEntity.uploaderLon,
          uploadedPhotoEntity.uploaderLat,
          receiverInfo,
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