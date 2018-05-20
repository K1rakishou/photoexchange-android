package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GetReceivedPhotosResponse
private constructor(

    @SerializedName("received_photos")
    val receivedPhotos: List<ReceivedPhoto>,

    errorCode: ErrorCode
) : StatusResponse(errorCode.getValue(), errorCode) {

    companion object {
        fun success(uploadedPhotos: List<ReceivedPhoto>): GetReceivedPhotosResponse {
            return GetReceivedPhotosResponse(uploadedPhotos, ErrorCode.GetReceivedPhotosErrors.Ok())
        }

        fun fail(errorCode: ErrorCode): GetReceivedPhotosResponse {
            return GetReceivedPhotosResponse(emptyList(), errorCode)
        }
    }

    //TODO: server size
    class ReceivedPhoto(

        @SerializedName("photo_id")
        val photoId: Long,

        @SerializedName("uploaded_photo_name")
        val uploadedPhotoName: String,

        @SerializedName("received_photo_name")
        val receivedPhotoName: String,

        @SerializedName("receiver_lon")
        val receiverLon: Double,

        @SerializedName("receiver_lat")
        val receiverLat: Double
    )
}