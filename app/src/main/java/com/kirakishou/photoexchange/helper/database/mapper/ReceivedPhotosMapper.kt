package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.mvp.model.PhotoAnswer
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse

object ReceivedPhotosMapper {

    fun toPhotoAnswer(receivedPhotoEntity: ReceivedPhotoEntity): PhotoAnswer {
        return PhotoAnswer(
            receivedPhotoEntity.id!!,
            receivedPhotoEntity.uploadedPhotoName!!,
            receivedPhotoEntity.receivedPhotoName!!,
            receivedPhotoEntity.lon!!,
            receivedPhotoEntity.lat!!
        )
    }

    fun toPhotoAnswer(id: Long?, receivedPhotosResponse: ReceivedPhotosResponse.ReceivedPhoto): PhotoAnswer {
        return PhotoAnswer(
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