package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoResponse(

    @Expose
    @SerializedName("photo_name")
    val photoName: String,

    errorCode: ErrorCode

) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun error(errorCode: ErrorCode): UploadPhotoResponse {
            return UploadPhotoResponse("", errorCode)
        }
    }
}