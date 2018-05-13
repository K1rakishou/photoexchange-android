package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GetUploadedPhotosResponse
private constructor(

    @Expose
    @SerializedName("uploaded_photos")
    val uploadedPhotos: List<UploadedPhotoData>,

    errorCode: ErrorCode
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(uploadedPhotos: List<UploadedPhotoData>): GetUploadedPhotosResponse {
            return GetUploadedPhotosResponse(uploadedPhotos, ErrorCode.GetUploadedPhotosError.Remote.Ok())
        }

        fun fail(errorCode: ErrorCode): GetUploadedPhotosResponse {
            return GetUploadedPhotosResponse(emptyList(), errorCode)
        }
    }

    class UploadedPhotoData(

        @Expose
        @SerializedName("photo_id")
        val photoId: Long,

        @Expose
        @SerializedName("photo_name")
        val photoName: String,

        @Expose
        @SerializedName("receiver_lon")
        val receiverLon: Double,

        @Expose
        @SerializedName("receiver_lat")
        val receiverLat: Double
    )
}