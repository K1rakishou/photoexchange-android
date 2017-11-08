package com.kirakishou.photoexchange.mvvm.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode


/**
 * Created by kirakishou on 11/3/2017.
 */
class UploadPhotoResponse(

    @Expose
    @SerializedName("photo_name")
    val photoName: String,

    serverErrorCode: ServerErrorCode
) : StatusResponse(serverErrorCode.value)