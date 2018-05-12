package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

open class ReceivePhotosResponse
private constructor(

    @Expose
    @SerializedName("photo_answers")
    val photoAnswers: List<PhotoAnswer>,

    errorCode: ErrorCode
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(photoAnswers: List<PhotoAnswer>): ReceivePhotosResponse {
            return ReceivePhotosResponse(photoAnswers, ErrorCode.ReceivePhotosErrors.Remote.Ok())
        }

        fun error(errorCode: ErrorCode): ReceivePhotosResponse {
            return ReceivePhotosResponse(emptyList(), errorCode)
        }
    }

    inner class PhotoAnswer(
        @Expose
        @SerializedName("uploaded_photo_name")
        val uploadedPhotoName: String,

        @Expose
        @SerializedName("photo_answer_name")
        val photoAnswerName: String,

        @Expose
        @SerializedName("lon")
        val lon: Double,

        @Expose
        @SerializedName("lat")
        val lat: Double
    )
}