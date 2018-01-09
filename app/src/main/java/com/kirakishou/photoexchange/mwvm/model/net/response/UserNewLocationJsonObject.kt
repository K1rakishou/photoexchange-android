package com.kirakishou.photoexchange.mwvm.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by kirakishou on 1/8/2018.
 */
class UserNewLocationJsonObject(
        @Expose
        @SerializedName("photo_name")
        val photoName: String,

        @Expose
        @SerializedName("lat")
        val lat: Double,

        @Expose
        @SerializedName("lon")
        val lon: Double
)