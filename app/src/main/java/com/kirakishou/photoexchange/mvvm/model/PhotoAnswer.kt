package com.kirakishou.photoexchange.mvvm.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by kirakishou on 11/12/2017.
 */
data class PhotoAnswer(
        val userId: String,
        val photoName: String
) {
}