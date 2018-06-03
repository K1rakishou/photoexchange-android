package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GetReceivedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse

object ReceivedPhotosMapper {

    object FromEntity {
        fun toReceivedPhoto(receivedPhotoEntity: ReceivedPhotoEntity): ReceivedPhoto {
            return ReceivedPhoto(
                receivedPhotoEntity.id!!,
                receivedPhotoEntity.uploadedPhotoName!!,
                receivedPhotoEntity.receivedPhotoName!!,
                receivedPhotoEntity.lon!!,
                receivedPhotoEntity.lat!!
            )
        }

        fun toReceivedPhotos(receivedPhotoEntityList: List<ReceivedPhotoEntity>): MutableList<ReceivedPhoto> {
            return receivedPhotoEntityList.map { toReceivedPhoto(it) } as MutableList<ReceivedPhoto>
        }
    }

    object FromResponse {
        object ReceivedPhotos {
            fun toReceivedPhoto(receivedPhotosResponse: ReceivedPhotosResponse.ReceivedPhoto): ReceivedPhoto {
                return ReceivedPhoto(
                    receivedPhotosResponse.photoId,
                    receivedPhotosResponse.uploadedPhotoName,
                    receivedPhotosResponse.receivedPhotoName,
                    receivedPhotosResponse.lon,
                    receivedPhotosResponse.lat
                )
            }

            fun toReceivedPhotos(receivedPhotosResponseList: List<ReceivedPhotosResponse.ReceivedPhoto>): List<ReceivedPhoto> {
                return receivedPhotosResponseList.map { toReceivedPhoto(it) }
            }

            fun toReceivedPhotoEntity(receivedPhotosResponse: ReceivedPhotosResponse.ReceivedPhoto): ReceivedPhotoEntity {
                return ReceivedPhotoEntity(
                    receivedPhotosResponse.photoId,
                    receivedPhotosResponse.uploadedPhotoName,
                    receivedPhotosResponse.receivedPhotoName,
                    receivedPhotosResponse.lon,
                    receivedPhotosResponse.lat
                )
            }

            fun toReceivedPhotoEntities(receivedPhotosResponseList: List<ReceivedPhotosResponse.ReceivedPhoto>): List<ReceivedPhotoEntity> {
                return receivedPhotosResponseList.map { toReceivedPhotoEntity(it) }
            }
        }

        object GetReceivedPhotos {
            fun toReceivedPhoto(receivedPhotosResponse: GetReceivedPhotosResponse.ReceivedPhoto): ReceivedPhoto {
                return ReceivedPhoto(
                    receivedPhotosResponse.photoId,
                    receivedPhotosResponse.uploadedPhotoName,
                    receivedPhotosResponse.receivedPhotoName,
                    receivedPhotosResponse.receiverLon,
                    receivedPhotosResponse.receiverLat
                )
            }

            fun toReceivedPhotos(receivedPhotosResponseList: List<GetReceivedPhotosResponse.ReceivedPhoto>): MutableList<ReceivedPhoto> {
                return receivedPhotosResponseList.map { toReceivedPhoto(it) } as MutableList<ReceivedPhoto>
            }

            fun toReceivedPhotoEntity(receivedPhotosResponse: GetReceivedPhotosResponse.ReceivedPhoto): ReceivedPhotoEntity {
                return ReceivedPhotoEntity.create(
                    receivedPhotosResponse.photoId,
                    receivedPhotosResponse.uploadedPhotoName,
                    receivedPhotosResponse.receivedPhotoName,
                    receivedPhotosResponse.receiverLon,
                    receivedPhotosResponse.receiverLat
                )
            }

            fun toReceivedPhotoEntities(receivedPhotosResponseList: List<GetReceivedPhotosResponse.ReceivedPhoto>): List<ReceivedPhotoEntity> {
                return receivedPhotosResponseList.map { toReceivedPhotoEntity(it) }
            }
        }
    }

    object FromObject {
        fun toPhotoAnswerEntity(receivedPhotos: GetReceivedPhotosResponse.ReceivedPhoto): ReceivedPhotoEntity {
            return ReceivedPhotoEntity(
                null,
                receivedPhotos.uploadedPhotoName,
                receivedPhotos.receivedPhotoName,
                receivedPhotos.receiverLon,
                receivedPhotos.receiverLat
            )
        }
    }
}