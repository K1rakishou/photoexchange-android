package com.kirakishou.photoexchange.mwvm.model.event

import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto

/**
 * Created by kirakishou on 11/7/2017.
 */
class PhotoUploadedEvent(
        val status: SendPhotoEventStatus,
        val photoId: Long
) : BaseEvent() {

    companion object {
        fun startUploading(): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.START, -1L)
        }

        fun photoUploaded(id: Long): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.PHOTO_UPLOADED, id)
        }

        fun fail(id: Long): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.FAIL, id)
        }

        fun done(): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.DONE, -1L)
        }
    }
}

