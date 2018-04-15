package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.mvp.model.PhotoAnswer
import com.kirakishou.photoexchange.mvp.model.net.response.PhotoAnswerResponse

object PhotoAnswerMapper {

    fun toPhotoAnswer(photoAnswerEntity: PhotoAnswerEntity): PhotoAnswer {
        return PhotoAnswer(
            photoAnswerEntity.id!!,
            photoAnswerEntity.photoAnswerName!!,
            photoAnswerEntity.lon!!,
            photoAnswerEntity.lat!!
        )
    }

    fun toPhotoAnswerEntity(photoAnswerJsonObject: PhotoAnswerResponse.PhotoAnswerJsonObject): PhotoAnswerEntity {
        return PhotoAnswerEntity(
            null,
            photoAnswerJsonObject.photoAnswerName,
            photoAnswerJsonObject.lon,
            photoAnswerJsonObject.lat
        )
    }
}