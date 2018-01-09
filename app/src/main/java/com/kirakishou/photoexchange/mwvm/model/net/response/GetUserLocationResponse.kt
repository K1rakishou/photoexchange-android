package com.kirakishou.photoexchange.mwvm.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode

/**
 * Created by kirakishou on 1/8/2018.
 */
class GetUserLocationResponse(
        @Expose
        @SerializedName("locations")
        val locationList: List<UserNewLocationJsonObject>,

        serverErrorCode: ServerErrorCode
) : StatusResponse(serverErrorCode.value) {
}