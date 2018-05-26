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
) : StatusResponse(errorCode.getValue(), errorCode) {

    companion object {
        fun success(uploadedPhotos: List<UploadedPhotoData>): GetUploadedPhotosResponse {
            return GetUploadedPhotosResponse(uploadedPhotos, ErrorCode.GetUploadedPhotosErrors.Ok())
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
        @SerializedName("uploader_lon")
        val uploaderLon: Double,

        @Expose
        @SerializedName("uploader_lat")
        val uploaderLat: Double,

        @Expose
        @SerializedName("has_receiver_info")
        val hasReceivedInfo: Boolean,

        @Expose
        @SerializedName("uploaded_on")
        val uploadedOn: Long
    )
}