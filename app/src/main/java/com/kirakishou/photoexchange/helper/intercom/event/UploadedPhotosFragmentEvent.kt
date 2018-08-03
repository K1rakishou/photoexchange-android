package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class UploadedPhotosFragmentEvent : BaseEvent {
    sealed class GeneralEvents : UploadedPhotosFragmentEvent() {
        class ShowProgressFooter : GeneralEvents()
        class HideProgressFooter : GeneralEvents()
        class RemovePhoto(val photo: TakenPhoto) : GeneralEvents()
        class AddPhoto(val photo: TakenPhoto) : GeneralEvents()
        class ScrollToTop : GeneralEvents()
        class PhotoRemoved : GeneralEvents()
        class AfterPermissionRequest : GeneralEvents()
        class UpdateReceiverInfo(val receivedPhotos: MutableList<ReceivedPhoto>) : GeneralEvents()
        class OnPageSelected : GeneralEvents()
        class DisableEndlessScrolling : GeneralEvents()
        class EnableEndlessScrolling : GeneralEvents()
        class PageIsLoading : GeneralEvents()
        class ShowTakenPhotos(val takenPhotos: List<TakenPhoto>) : GeneralEvents()
        class ShowUploadedPhotos(val uploadedPhotos: List<UploadedPhoto>) : GeneralEvents()
    }

    sealed class PhotoUploadEvent : UploadedPhotosFragmentEvent() {
        class OnFailedToUpload(val photo: TakenPhoto,
                               val errorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadEvent()
        class OnKnownError(val errorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadEvent()
        class OnUnknownError(val error: Throwable) : PhotoUploadEvent()
        class OnPhotoUploadStart(val photo: TakenPhoto) : PhotoUploadEvent()
        class OnProgress(val photo: TakenPhoto, val progress: Int) : PhotoUploadEvent()
        class OnUploaded(val takenPhoto: TakenPhoto) : PhotoUploadEvent()
        class PhotoReceived(val takenPhotoName: String) : PhotoUploadEvent()
        class OnEnd : PhotoUploadEvent()
    }
}