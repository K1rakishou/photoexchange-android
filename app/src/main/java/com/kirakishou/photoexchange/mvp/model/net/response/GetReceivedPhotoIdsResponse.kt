package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GetReceivedPhotoIdsResponse
private constructor(

    @Expose
    @SerializedName("received_photo_ids")
    val receivedPhotoIds: List<Long>,

    errorCode: ErrorCode.GetReceivedPhotosErrors
) : StatusResponse(errorCode.getValue(), errorCode) {

    companion object {
        fun success(uploadedPhotoIds: List<Long>): GetReceivedPhotoIdsResponse {
            return GetReceivedPhotoIdsResponse(uploadedPhotoIds, ErrorCode.GetReceivedPhotosErrors.Ok())
        }

        fun fail(errorCode: ErrorCode.GetReceivedPhotosErrors): GetReceivedPhotoIdsResponse {
            return GetReceivedPhotoIdsResponse(emptyList(), errorCode)
        }
    }
}