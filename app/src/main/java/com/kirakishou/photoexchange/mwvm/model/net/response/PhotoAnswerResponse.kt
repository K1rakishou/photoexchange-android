package com.kirakishou.photoexchange.mwvm.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode

/**
 * Created by kirakishou on 11/12/2017.
 */
class PhotoAnswerResponse(

    @Expose
    @SerializedName("photo_answer")
    val photoAnswer: PhotoAnswerJsonObject?,

    @Expose
    @SerializedName("all_found")
    val allFound: Boolean,

    serverErrorCode: ServerErrorCode

) : StatusResponse(serverErrorCode.value) {

    companion object {
        fun error(errorCode: ServerErrorCode): PhotoAnswerResponse {
            return PhotoAnswerResponse(null, false, errorCode)
        }
    }
}