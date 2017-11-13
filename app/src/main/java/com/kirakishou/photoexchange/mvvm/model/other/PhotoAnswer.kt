package com.kirakishou.photoexchange.mvvm.model.other

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by kirakishou on 11/13/2017.
 */
data class PhotoAnswer(
        val userId: String,
        val photoName: String,
        val lon: Double,
        val lat: Double
) {
}