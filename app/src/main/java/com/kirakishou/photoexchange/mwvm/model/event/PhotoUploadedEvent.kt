package com.kirakishou.photoexchange.mwvm.model.event

import com.kirakishou.photoexchange.mwvm.model.other.UploadedPhoto

/**
 * Created by kirakishou on 11/7/2017.
 */
class PhotoUploadedEvent(
        val status: SendPhotoEventStatus,
        val photo: UploadedPhoto?

) : BaseEvent() {

    companion object {
        fun success(photo: UploadedPhoto?): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.SUCCESS, photo)
        }

        fun fail(): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.FAIL, null)
        }
    }
}

