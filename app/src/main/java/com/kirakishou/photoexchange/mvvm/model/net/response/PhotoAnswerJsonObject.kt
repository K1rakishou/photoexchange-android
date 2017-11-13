package com.kirakishou.photoexchange.mvvm.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by kirakishou on 11/12/2017.
 */
data class PhotoAnswerJsonObject(

        @Expose
        @SerializedName("user_id")
        val userId: String,

        @Expose
        @SerializedName("photo_name")
        val photoName: String,

        @Expose
        @SerializedName("lon")
        val lon: Double,

        @Expose
        @SerializedName("lat")
        val lat: Double
) {
}