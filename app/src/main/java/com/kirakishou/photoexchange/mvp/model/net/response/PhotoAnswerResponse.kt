package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

open class PhotoAnswerResponse
private constructor(

    @Expose
    @SerializedName("photo_answers")
    val photoAnswers: List<PhotoAnswerJsonObject>,

    @Expose
    @SerializedName("all_found")
    val allFound: Boolean,

    errorCode: ErrorCode
) : StatusResponse(errorCode.value) {

    companion object {
        fun error(errorCode: ErrorCode): PhotoAnswerResponse {
            return PhotoAnswerResponse(emptyList(), false, errorCode)
        }
    }

    inner class PhotoAnswerJsonObject(
        @Expose
        @SerializedName("user_id")
        val userId: String,

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