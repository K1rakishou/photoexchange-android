package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.mvp.model.PhotoAnswer
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivePhotosResponse

object PhotoAnswerMapper {

    fun toPhotoAnswer(photoAnswerEntity: PhotoAnswerEntity): PhotoAnswer {
        return PhotoAnswer(
            photoAnswerEntity.id!!,
            photoAnswerEntity.uploadedPhotoName!!,
            photoAnswerEntity.photoAnswerName!!,
            photoAnswerEntity.lon!!,
            photoAnswerEntity.lat!!
        )
    }

    fun toPhotoAnswer(id: Long?, receivePhotosResponse: ReceivePhotosResponse.PhotoAnswer): PhotoAnswer {
        return PhotoAnswer(
            id,
            receivePhotosResponse.uploadedPhotoName,
            receivePhotosResponse.photoAnswerName,
            receivePhotosResponse.lon,
            receivePhotosResponse.lat
        )
    }

    fun toPhotoAnswerEntity(receivePhotos: ReceivePhotosResponse.PhotoAnswer): PhotoAnswerEntity {
        return PhotoAnswerEntity(
            null,
            receivePhotos.uploadedPhotoName,
            receivePhotos.photoAnswerName,
            receivePhotos.lon,
            receivePhotos.lat
        )
    }
}