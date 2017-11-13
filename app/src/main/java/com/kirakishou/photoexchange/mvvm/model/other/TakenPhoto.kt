package com.kirakishou.photoexchange.mvvm.model.other

/**
 * Created by kirakishou on 11/10/2017.
 */
class TakenPhoto(
        val location: LonLat,
        val photoFilePath: String,
        val userId: String
) {

    fun isEmpty(): Boolean {
        return location.isEmpty() && photoFilePath.isEmpty() && userId.isEmpty()
    }

    companion object {
        fun empty(): TakenPhoto {
            return TakenPhoto(LonLat.empty(), "", "")
        }
    }
}