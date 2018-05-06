package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.mvp.model.PhotoAnswer
import com.kirakishou.photoexchange.mvp.model.net.response.PhotoAnswerResponse

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

    fun toPhotoAnswer(id: Long?, photoAnswerResponse: PhotoAnswerResponse.PhotoAnswer): PhotoAnswer {
        return PhotoAnswer(
            id,
            photoAnswerResponse.uploadedPhotoName,
            photoAnswerResponse.photoAnswerName,
            photoAnswerResponse.lon,
            photoAnswerResponse.lat
        )
    }

    fun toPhotoAnswerEntity(photoAnswer: PhotoAnswerResponse.PhotoAnswer): PhotoAnswerEntity {
        return PhotoAnswerEntity(
            null,
            photoAnswer.uploadedPhotoName,
            photoAnswer.photoAnswerName,
            photoAnswer.lon,
            photoAnswer.lat
        )
    }
}