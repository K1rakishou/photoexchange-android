package com.kirakishou.photoexchange.helper.mapper

import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer

/**
 * Created by kirakishou on 11/14/2017.
 */
class PhotoAnswerMapper {

    fun toPhotoAnswer(photoAnswerEntity: PhotoAnswerEntity) =
            PhotoAnswer.fromPhotoAnswerEntity(photoAnswerEntity)

    fun toPhotoAnswers(photoAnswerEntityList: List<PhotoAnswerEntity>) =
            photoAnswerEntityList.map { toPhotoAnswer(it) }
}