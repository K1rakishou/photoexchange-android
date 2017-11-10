package com.kirakishou.photoexchange.mvvm.model

/**
 * Created by kirakishou on 11/8/2017.
 */
data class UploadedPhoto(
        var id: Long,
        var lon: Double,
        var lat: Double,
        var userId: String,
        var photoName: String,
        var photoFilePath: String
) {
    fun isEmpty(): Boolean {
        return id == -1L && lon == 0.0 && lat == 0.0 &&
                userId.isEmpty() && photoName.isEmpty() &&
                photoFilePath.isEmpty()
    }

    companion object {
        fun empty(): UploadedPhoto {
            return UploadedPhoto(-1L, 0.0, 0.0, "",
                    "", "")
        }
    }
}