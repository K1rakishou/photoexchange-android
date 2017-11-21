package com.kirakishou.photoexchange.mwvm.model.event

import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer

/**
 * Created by kirakishou on 11/17/2017.
 */
class PhotoReceivedEvent(
        val status: PhotoReceivedEventStatus,
        val photoAnswer: PhotoAnswer?,
        val allFound: Boolean = false
) : BaseEvent() {

    companion object {
        fun successAllReceived(photoAnswer: PhotoAnswer): PhotoReceivedEvent {
            return PhotoReceivedEvent(PhotoReceivedEventStatus.SUCCESS_ALL_RECEIVED, photoAnswer, true)
        }

        fun successNotAllReceived(photoAnswer: PhotoAnswer): PhotoReceivedEvent {
            return PhotoReceivedEvent(PhotoReceivedEventStatus.SUCCESS_NOT_ALL_RECEIVED, photoAnswer)
        }

        fun fail(): PhotoReceivedEvent {
            return PhotoReceivedEvent(PhotoReceivedEventStatus.FAIL, null)
        }

        fun noPhotos(): PhotoReceivedEvent {
            return PhotoReceivedEvent(PhotoReceivedEventStatus.NO_PHOTOS_ON_SERVER, null)
        }

        fun uploadMorePhotos(): PhotoReceivedEvent {
            return PhotoReceivedEvent(PhotoReceivedEventStatus.UPLOAD_MORE_PHOTOS, null)
        }
    }
}