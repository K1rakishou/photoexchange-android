package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ServerErrorCode

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoResponse(

    @Expose
    @SerializedName("photo_name")
    val photoName: String,

    serverErrorCode: ServerErrorCode

) : StatusResponse(serverErrorCode.value) {

    companion object {
        fun error(errorCode: ServerErrorCode): UploadPhotoResponse {
            return UploadPhotoResponse("", errorCode)
        }
    }
}