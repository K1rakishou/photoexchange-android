package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class UploadedPhotosFragmentEvent : BaseEvent {
    sealed class UiEvents : UploadedPhotosFragmentEvent() {
        class ShowObtainCurrentLocationNotification : UiEvents()
        class HideObtainCurrentLocationNotification : UiEvents()
        class ShowProgressFooter : UiEvents()
        class HideProgressFooter : UiEvents()
        class RemovePhoto(val photo: TakenPhoto) : UiEvents()
        class AddPhoto(val photo: TakenPhoto) : UiEvents()
        class ScrollToTop : UiEvents()
    }

    sealed class PhotoUploadEvent : UploadedPhotosFragmentEvent() {
        class OnLocationUpdateStart : PhotoUploadEvent()
        class OnLocationUpdateEnd : PhotoUploadEvent()
        class OnFailedToUpload(val photo: TakenPhoto, val errorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadEvent()
        class OnUnknownError(val error: Throwable) : PhotoUploadEvent()
        class OnCouldNotGetUserIdFromServerError(val errorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadEvent()
        class OnPrepare : PhotoUploadEvent()
        class OnPhotoUploadStart(val photo: TakenPhoto) : PhotoUploadEvent()
        class OnProgress(val photo: TakenPhoto, val progress: Int) : PhotoUploadEvent()
        class OnUploaded(val photo: UploadedPhoto) : PhotoUploadEvent()
        class OnFoundPhotoAnswer(val takenPhotoName: String) : PhotoUploadEvent()
        class OnEnd(val allUploaded: Boolean) : PhotoUploadEvent()
    }
}