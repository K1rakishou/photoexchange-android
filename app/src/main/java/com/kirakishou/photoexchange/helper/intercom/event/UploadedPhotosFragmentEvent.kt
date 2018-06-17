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
        class PhotoRemoved : UiEvents()
        class LoadPhotos : UiEvents()
        class UpdateReceiverInfo(val receivedPhotos: MutableList<ReceivedPhoto>) : UiEvents()
    }

    sealed class PhotoUploadEvent : UploadedPhotosFragmentEvent() {
        class OnFailedToUpload(val photo: TakenPhoto,
                               val errorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadEvent()
        class OnKnownError(val errorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadEvent()
        class OnUnknownError(val error: Throwable) : PhotoUploadEvent()
        class OnPhotoUploadStart(val photo: TakenPhoto) : PhotoUploadEvent()
        class OnProgress(val photo: TakenPhoto, val progress: Int) : PhotoUploadEvent()
        class OnUploaded(val takenPhoto: TakenPhoto) : PhotoUploadEvent()
        class PhotoAnswerFound(val takenPhotoName: String) : PhotoUploadEvent()
        class OnEnd : PhotoUploadEvent()
    }
}