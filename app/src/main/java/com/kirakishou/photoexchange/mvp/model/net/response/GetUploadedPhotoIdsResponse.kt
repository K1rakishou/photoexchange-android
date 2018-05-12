package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GetUploadedPhotoIdsResponse
private constructor(

    @Expose
    @SerializedName("uploaded_photo_ids")
    val uploadedPhotoIds: List<Long>,

    errorCode: ErrorCode
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(uploadedPhotoIds: List<Long>): GetUploadedPhotoIdsResponse {
            return GetUploadedPhotoIdsResponse(uploadedPhotoIds, ErrorCode.GetUploadedPhotoIdsError.Remote.Ok())
        }

        fun fail(errorCode: ErrorCode): GetUploadedPhotoIdsResponse {
            return GetUploadedPhotoIdsResponse(emptyList(), errorCode)
        }
    }
}