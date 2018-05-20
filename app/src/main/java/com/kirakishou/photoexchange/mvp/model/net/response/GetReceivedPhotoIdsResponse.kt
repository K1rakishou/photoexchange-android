package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GetReceivedPhotoIdsResponse
private constructor(

    @SerializedName("received_photo_ids")
    val receivedPhotoIds: List<Long>,

    errorCode: ErrorCode
) : StatusResponse(errorCode.getValue(), errorCode) {

    companion object {
        fun success(uploadedPhotoIds: List<Long>): GetReceivedPhotoIdsResponse {
            return GetReceivedPhotoIdsResponse(uploadedPhotoIds, ErrorCode.GetReceivedPhotosErrors.Ok())
        }

        fun fail(errorCode: ErrorCode): GetReceivedPhotoIdsResponse {
            return GetReceivedPhotoIdsResponse(emptyList(), errorCode)
        }
    }
}