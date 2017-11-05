package com.kirakishou.photoexchange.mvvm.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode

/**
 * Created by kirakishou on 11/3/2017.
 */
open class StatusResponse(
        @Expose
        @SerializedName("server_error_code")
        var serverErrorCode: Int?
)