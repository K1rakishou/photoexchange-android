package com.kirakishou.photoexchange.mwvm.model.state

/**
 * Created by kirakishou on 11/30/2017.
 */
enum class PhotoState(val value: String) {
    TAKEN(PhotoState.TAKEN_PHOTO_STATE),
    QUEUED_UP(PhotoState.QUEUED_UP_STATE),
    UPLOADING(PhotoState.UPLOADING_STATE),
    FAILED_TO_UPLOAD(PhotoState.FAILED_TO_UPLOAD_STATE),
    UPLOADED(PhotoState.UPLOADED_STATE);

    companion object {
        const val TAKEN_PHOTO_STATE = "taken"
        const val QUEUED_UP_STATE = "queued_up"
        const val UPLOADING_STATE = "uploading"
        const val FAILED_TO_UPLOAD_STATE = "failed_to_upload"
        const val UPLOADED_STATE = "uploaded"

        fun from(stateStr: String): PhotoState {
            for (photoState in PhotoState.values()) {
                if (photoState.value == stateStr) {
                    return photoState
                }
            }

            throw IllegalStateException("Could not find value $stateStr in PhotoState")
        }
    }
}