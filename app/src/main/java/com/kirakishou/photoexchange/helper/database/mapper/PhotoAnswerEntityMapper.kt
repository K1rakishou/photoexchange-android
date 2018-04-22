package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.mvp.model.PhotoAnswer

object PhotoAnswerEntityMapper {

    fun toPhotoAnswer(photoAnswerEntity: PhotoAnswerEntity): PhotoAnswer {
        return PhotoAnswer(
            photoAnswerEntity.id!!,
            photoAnswerEntity.uploadedPhotoName!!,
            photoAnswerEntity.photoAnswerName!!,
            photoAnswerEntity.lon!!,
            photoAnswerEntity.lat!!
        )
    }
}