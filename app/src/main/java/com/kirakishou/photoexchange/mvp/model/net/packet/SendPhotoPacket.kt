package com.kirakishou.photoexchange.mvp.model.net.packet

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by kirakishou on 3/17/2018.
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