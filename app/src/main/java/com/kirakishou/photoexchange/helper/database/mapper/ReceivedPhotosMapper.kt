package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse

object ReceivedPhotosMapper {

    fun toPhotoAnswer(receivedPhotoEntity: ReceivedPhotoEntity): ReceivedPhoto {
        return ReceivedPhoto(
            receivedPhotoEntity.id!!,
            receivedPhotoEntity.uploadedPhotoName!!,
            receivedPhotoEntity.receivedPhotoName!!,
            receivedPhotoEntity.lon!!,
            receivedPhotoEntity.lat!!
        )
    }

    fun toPhotoAnswer(id: Long?, receivedPhotosResponse: ReceivedPhotosResponse.ReceivedPhoto): ReceivedPhoto {
        return ReceivedPhoto(
            id,
            receivedPhotosResponse.uploadedPhotoName,
            receivedPhotosResponse.receivedPhotoName,
            receivedPhotosResponse.lon,
            receivedPhotosResponse.lat
        )
    }

    fun toPhotoAnswerEntity(receivedPhotos: ReceivedPhotosResponse.ReceivedPhoto): ReceivedPhotoEntity {
        return ReceivedPhotoEntity(
            null,
            receivedPhotos.uploadedPhotoName,
            receivedPhotos.receivedPhotoName,
            receivedPhotos.lon,
            receivedPhotos.lat
        )
    }
}