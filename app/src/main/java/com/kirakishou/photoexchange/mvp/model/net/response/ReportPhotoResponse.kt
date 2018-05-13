package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class ReportPhotoResponse
private constructor(

    @Expose
    @SerializedName("is_reported")
    val isReported: Boolean,

    errorCode: ErrorCode
) : StatusResponse(errorCode.getValue(), errorCode) {

    companion object {
        fun success(isReported: Boolean): ReportPhotoResponse {
            return ReportPhotoResponse(isReported, ErrorCode.ReportPhotoErrors.Ok())
        }

        fun error(errorCode: ErrorCode): ReportPhotoResponse {
            return ReportPhotoResponse(false, errorCode)
        }
    }
}