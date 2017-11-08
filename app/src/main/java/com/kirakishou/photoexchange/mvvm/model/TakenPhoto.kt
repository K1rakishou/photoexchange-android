package com.kirakishou.photoexchange.mvvm.model

import android.arch.persistence.room.ColumnInfo

/**
 * Created by kirakishou on 11/8/2017.
 */
data class TakenPhoto(
        var id: Long,
        var lon: Double,
        var lat: Double,
        var userId: String,
        var photoName: String,
        var photoFilePath: String,
        var wasSent: Boolean
) {
    fun isEmpty(): Boolean {
        return id == -1L && lon == 0.0 && lat == 0.0 &&
                userId.isEmpty() && photoName.isEmpty() &&
                photoFilePath.isEmpty() && !wasSent
    }

    companion object {
        fun empty(): TakenPhoto {
            return TakenPhoto(-1L, 0.0, 0.0, "",
                    "", "", false)
        }
    }
}