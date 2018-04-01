package com.kirakishou.photoexchange.mvp.model

import com.kirakishou.photoexchange.mvp.model.other.LonLat

class UploadPhotoData(
    val empty: Boolean,
    val userId: String,
    val location: LonLat
) {
    fun isEmpty(): Boolean {
        return empty
    }

    companion object {
        fun empty(): UploadPhotoData {
            return UploadPhotoData(true, "", LonLat.empty())
        }
    }
}