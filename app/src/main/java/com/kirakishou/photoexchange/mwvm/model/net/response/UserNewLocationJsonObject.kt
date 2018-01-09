package com.kirakishou.photoexchange.mwvm.model.net.response

import com.google.gson.annotations.SerializedName

/**
 * Created by kirakishou on 1/8/2018.
 */
class UserNewLocationJsonObject(
        @SerializedName("photo_name")
        val photoName: String,

        @SerializedName("lat")
        val lat: Double,

        @SerializedName("lon")
        val lon: Double
)