package com.kirakishou.photoexchange.mvvm.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode

/**
 * Created by kirakishou on 11/12/2017.
 */
class PhotoAnswerResponse(

        @Expose
        @SerializedName("user_id")
        val userId: String,

        @Expose
        @SerializedName("photo_name")
        val photoName: String,

        serverErrorCode: ServerErrorCode

) : StatusResponse(serverErrorCode.value) {

}