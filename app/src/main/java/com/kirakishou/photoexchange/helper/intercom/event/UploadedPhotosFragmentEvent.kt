package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class UploadedPhotosFragmentEvent : BaseEvent {
    sealed class UiEvents : UploadedPhotosFragmentEvent() {
        class ShowProgressFooter : UiEvents()
        class HideProgressFooter : UiEvents()
        class RemovePhoto(val photo: TakenPhoto) : UiEvents()
        class AddPhoto(val photo: TakenPhoto) : UiEvents()
        class ScrollToTop : UiEvents()
        class OnPhotoRemoved : UiEvents()
        class LoadTakenPhotos : UiEvents()
        class UpdateReceiverInfo(val receivedPhotos: MutableList<ReceivedPhoto>) : UiEvents()
    }

    sealed class PhotoUploadEvent : UploadedPhotosFragmentEvent() {
        class OnFailedToUpload(val photo: TakenPhoto,
                               val errorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadEvent()
        class OnError(val error: UploadingError) : PhotoUploadEvent()
        class OnPhotoUploadStart(val photo: TakenPhoto) : PhotoUploadEvent()
        class OnProgress(val photo: TakenPhoto, val progress: Int) : PhotoUploadEvent()
        class OnUploaded(val takenPhoto: TakenPhoto,
                         val uploadedPhoto: UploadedPhoto) : PhotoUploadEvent()
        class OnFoundPhotoAnswer(val takenPhotoName: String) : PhotoUploadEvent()
        class OnEnd(val allUploaded: Boolean) : PhotoUploadEvent()
    }

    companion object {
        fun unknownError(error: Throwable): PhotoUploadEvent.OnError {
            return PhotoUploadEvent.OnError(UploadingError.UnknownError(error))
        }

        fun knownError(errorCode: ErrorCode.UploadPhotoErrors): PhotoUploadEvent.OnError {
            return PhotoUploadEvent.OnError(UploadingError.KnownError(errorCode))
        }
    }

    sealed class UploadingError {
        data class KnownError(val errorCode: ErrorCode.UploadPhotoErrors) : UploadingError()
        data class UnknownError(val error: Throwable) : UploadingError()
    }
}