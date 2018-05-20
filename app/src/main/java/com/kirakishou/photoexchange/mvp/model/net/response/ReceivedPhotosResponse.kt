package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

open class ReceivedPhotosResponse
private constructor(

    @Expose
    @SerializedName("received_photos")
    val receivedPhotos: List<ReceivedPhoto>,

    errorCode: ErrorCode
) : StatusResponse(errorCode.getValue(), errorCode) {

    companion object {
        fun success(receivedPhotos: List<ReceivedPhoto>): ReceivedPhotosResponse {
            return ReceivedPhotosResponse(receivedPhotos, ErrorCode.ReceivePhotosErrors.Ok())
        }

        fun error(errorCode: ErrorCode): ReceivedPhotosResponse {
            return ReceivedPhotosResponse(emptyList(), errorCode)
        }
    }

    //TODO: add photoId to the response on the server side as well
    inner class ReceivedPhoto(
        @Expose
        @SerializedName("photo_id")
        val photoId: Long,

        @Expose
        @SerializedName("uploaded_photo_name")
        val uploadedPhotoName: String,

        @Expose
        @SerializedName("received_photo_name")
        val receivedPhotoName: String,

        @Expose
        @SerializedName("lon")
        val lon: Double,

        @Expose
        @SerializedName("lat")
        val lat: Double
    )
}