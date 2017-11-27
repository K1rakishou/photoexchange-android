package com.kirakishou.photoexchange.mwvm.model.other

/**
 * Created by kirakishou on 11/10/2017.
 */
data class TakenPhoto(
        val id: Long,
        val location: LonLat,
        val photoFilePath: String,
        val userId: String,
        var photoName: String
) {

    fun isEmpty(): Boolean {
        return id == -1L
    }

    fun copy(id: Long): TakenPhoto {
        return TakenPhoto(id, location, photoFilePath, userId, photoName)
    }

    companion object {
        fun empty(): TakenPhoto {
            return TakenPhoto(-1L, LonLat.empty(), "", "", "")
        }
    }
}