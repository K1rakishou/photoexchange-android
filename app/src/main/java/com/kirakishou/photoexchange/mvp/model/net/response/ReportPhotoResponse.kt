package com.kirakishou.photoexchange.mvp.model.net.response

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class ReportPhotoResponse
private constructor(
    errorCode: ErrorCode.ReportPhotoErrors
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(): ReportPhotoResponse {
            return ReportPhotoResponse(ErrorCode.ReportPhotoErrors.Remote.Ok())
        }

        fun error(errorCode: ErrorCode.ReportPhotoErrors): ReportPhotoResponse {
            return ReportPhotoResponse(errorCode)
        }
    }
}