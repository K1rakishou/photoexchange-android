package com.kirakishou.photoexchange.mwvm.model.other

import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.mwvm.model.net.response.PhotoAnswerJsonObject

/**
 * Created by kirakishou on 11/13/2017.
 */
data class PhotoAnswer(
        val photoRemoteId: Long,
        val userId: String,
        val photoName: String,
        val lon: Double,
        val lat: Double
) {

    fun isAnonymous(): Boolean {
        return LonLat(lon, lat).isEmpty()
    }

    companion object {
        fun fromPhotoAnswerJsonObject(answer: PhotoAnswerJsonObject) =
                PhotoAnswer(answer.id, answer.userId, answer.photoName, answer.lon, answer.lat)

        fun fromPhotoAnswerEntity(answer: PhotoAnswerEntity) =
                PhotoAnswer(answer.photoRemoteId, answer.userId, answer.photoName, answer.lon, answer.lat)
    }
}