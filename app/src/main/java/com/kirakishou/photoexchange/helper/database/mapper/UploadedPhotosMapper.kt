package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotosResponse

object UploadedPhotosMapper {

    object FromResponse {
        object ToEntity {
            fun toUploadedPhotoEntity(time: Long, uploadedPhotoData: GetUploadedPhotosResponse.UploadedPhotoData): UploadedPhotoEntity {
                return UploadedPhotoEntity.create(
                    uploadedPhotoData.photoName,
                    uploadedPhotoData.photoId,
                    uploadedPhotoData.uploaderLon,
                    uploadedPhotoData.uploaderLat,
                    uploadedPhotoData.hasReceivedInfo,
                    time,
                    uploadedPhotoData.uploadedOn
                )
            }

            fun toUploadedPhotoEntities(time: Long, uploadedPhotoDataList: List<GetUploadedPhotosResponse.UploadedPhotoData>): List<UploadedPhotoEntity> {
                return uploadedPhotoDataList.map { toUploadedPhotoEntity(time, it) }
            }
        }

        object ToObject {
            fun toUploadedPhoto(uploadedPhotoData: GetUploadedPhotosResponse.UploadedPhotoData): UploadedPhoto {
                return UploadedPhoto(
                    uploadedPhotoData.photoId,
                    uploadedPhotoData.photoName,
                    uploadedPhotoData.uploaderLon,
                    uploadedPhotoData.uploaderLat,
                    uploadedPhotoData.hasReceivedInfo,
                    uploadedPhotoData.uploadedOn
                )
            }

            fun toUploadedPhotos(uploadedPhotoDataList: List<GetUploadedPhotosResponse.UploadedPhotoData>): List<UploadedPhoto> {
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
                    uploadedPhotoEntity.hasReceiverInfo,
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
                return UploadedPhotoEntity.create(
                    photoName,
                    photoId,
                    lon,
                    lat,
                    false,
                    time,
                    uploadedOn
                )
            }
        }
    }
}