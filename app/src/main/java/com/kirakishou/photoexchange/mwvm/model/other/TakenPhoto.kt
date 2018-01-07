package com.kirakishou.photoexchange.mwvm.model.other

import com.kirakishou.photoexchange.mwvm.model.state.PhotoState

/**
 * Created by kirakishou on 11/10/2017.
 */
class TakenPhoto private constructor(
        val id: Long,
        val location: LonLat,
        val photoFilePath: String,
        val userId: String,
        var photoName: String,
        val photoState: PhotoState
) {

    fun isEmpty(): Boolean {
        return id == -1L
    }

    fun copy(_photoFilePath: String, _location: LonLat): TakenPhoto {
        return TakenPhoto(id, _location, _photoFilePath, userId, photoName, photoState)
    }

    fun copy(_id: Long): TakenPhoto {
        return TakenPhoto(_id, location, photoFilePath, userId, photoName, photoState)
    }

    override fun toString(): String {
        return "[id: $id, location: $location, photoFilePath: $photoFilePath, " +
                "userId: $userId, photoName: $photoName, photoState: $photoState]"
    }

    companion object {
        fun empty(): TakenPhoto {
            return TakenPhoto(-1L, LonLat.empty(), "", "", "", PhotoState.TAKEN)
        }

        fun create(photoFilePath: String, userId: String): TakenPhoto {
            return TakenPhoto(-1L, LonLat.empty(), photoFilePath, userId, "", PhotoState.TAKEN)
        }

        fun create(id: Long, location: LonLat, photoFilePath: String, userId: String, photoName: String, photoState: PhotoState): TakenPhoto {
            return TakenPhoto(id, location, photoFilePath, userId, photoName, photoState)
        }
    }
}