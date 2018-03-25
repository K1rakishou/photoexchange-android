package com.kirakishou.photoexchange.mvp.model

/**
 * Created by kirakishou on 3/8/2018.
 */
enum class PhotoState(val state: Int) {
    PHOTO_TAKEN(0),
    PHOTO_TO_BE_UPLOADED(1),
    PHOTO_UPLOADING(2),
    PHOTO_UPLOADED(3);

    companion object {
        fun from(state: Int): PhotoState {
            val result = PhotoState.values().firstOrNull { it.state == state }
            if (result == null) {
                throw RuntimeException("Unknown state $state")
            }

            return result
        }
    }
}