package com.kirakishou.photoexchange.mwvm.model.event

import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.model.event.status.SendPhotoEventStatus

/**
 * Created by kirakishou on 11/7/2017.
 */
class PhotoUploadedEvent(
        val status: SendPhotoEventStatus,
        val photo: TakenPhoto?
) : BaseEvent() {

    companion object {
        fun startUploading(): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.START, null)
        }

        fun photoUploaded(photo: TakenPhoto): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.PHOTO_UPLOADED, photo)
        }

        fun fail(photo: TakenPhoto): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.FAIL, photo)
        }

        fun done(): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.DONE, null)
        }
    }
}

