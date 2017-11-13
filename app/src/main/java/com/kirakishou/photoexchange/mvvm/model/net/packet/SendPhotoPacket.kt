package com.kirakishou.photoexchange.mvvm.model.net.packet

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by kirakishou on 11/3/2017.
 */
class SendPhotoPacket(
        @Expose
        @SerializedName("lon")
        val lon: Double,

        @Expose
        @SerializedName("lat")
        val lat: Double,

        @Expose
        @SerializedName("user_id")
        val userId: String
)