package com.kirakishou.photoexchange.mwvm.model.event

import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto

/**
 * Created by kirakishou on 11/7/2017.
 */
class PhotoUploadedEvent(
        val status: SendPhotoEventStatus,
        val photoId: Long,
        val photosToUpload: List<Long>

) : BaseEvent() {

    companion object {
        fun startUploading(ids: List<Long>): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.START, -1L, ids)
        }

        fun photoUploaded(id: Long): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.PHOTO_UPLOADED, id, emptyList())
        }

        fun fail(id: Long): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.FAIL, id, emptyList())
        }

        fun done(): PhotoUploadedEvent {
            return PhotoUploadedEvent(SendPhotoEventStatus.DONE, -1L, emptyList())
        }
    }
}

