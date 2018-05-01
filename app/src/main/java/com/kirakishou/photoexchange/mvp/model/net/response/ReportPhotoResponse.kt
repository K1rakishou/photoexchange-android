package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class ReportPhotoResponse
private constructor(

    @Expose
    @SerializedName("is_reported")
    val isReported: Boolean,

    errorCode: ErrorCode.ReportPhotoErrors
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(isReported: Boolean): ReportPhotoResponse {
            return ReportPhotoResponse(isReported, ErrorCode.ReportPhotoErrors.Remote.Ok())
        }

        fun error(errorCode: ErrorCode.ReportPhotoErrors): ReportPhotoResponse {
            return ReportPhotoResponse(false, errorCode)
        }
    }
}