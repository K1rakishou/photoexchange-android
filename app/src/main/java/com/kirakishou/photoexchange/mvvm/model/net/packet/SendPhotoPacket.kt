package com.kirakishou.photoexchange.mvvm.model.net.packet

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvvm.model.LonLat

/**
 * Created by kirakishou on 11/3/2017.
 */
class SendPhotoPacket(
        @Expose
        @SerializedName("location")
        val location: LonLat
)