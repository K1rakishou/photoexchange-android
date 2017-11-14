package com.kirakishou.photoexchange.mwvm.model.other

import com.kirakishou.photoexchange.mwvm.model.net.response.PhotoAnswerJsonObject

/**
 * Created by kirakishou on 11/13/2017.
 */
data class PhotoAnswer(
        val userId: String,
        val photoName: String,
        val lon: Double,
        val lat: Double
) {

    companion object {
        fun fromPhotoAnswerJsonObject(answer: PhotoAnswerJsonObject) =
                PhotoAnswer(answer.userId, answer.photoName, answer.lon, answer.lat)
    }
}