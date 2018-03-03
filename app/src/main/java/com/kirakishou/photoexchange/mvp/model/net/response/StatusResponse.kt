package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by kirakishou on 3/3/2018.
 */
open class StatusResponse(
    @Expose
    @SerializedName("server_error_code")
    var serverErrorCode: Int?
)