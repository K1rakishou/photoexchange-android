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

        fun success(id: Long): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.SUCCESS, id)
        }

        fun fail(): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.FAIL, -1L)
        }

        fun done(): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.DONE, -1L)
        }
    }
}

