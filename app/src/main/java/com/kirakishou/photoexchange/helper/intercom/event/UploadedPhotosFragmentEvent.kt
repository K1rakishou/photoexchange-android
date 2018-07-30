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
        class AddFailedToUploadPhotos(val photos: List<TakenPhoto>) : GeneralEvents()
        class AddQueuedUpPhotos(val photos: List<TakenPhoto>) : GeneralEvents()
        class AddUploadedPhotos(val photos: List<UploadedPhoto>) : GeneralEvents()
        class ScrollToTop : GeneralEvents()
        class PhotoRemoved : GeneralEvents()
        class AfterPermissionRequest : GeneralEvents()
        class UpdateReceiverInfo(val receivedPhotos: MutableList<ReceivedPhoto>) : GeneralEvents()
        class ClearAdapter : GeneralEvents()
        class StartRefreshing : GeneralEvents()
        class StopRefreshing : GeneralEvents()
        class OnTabClicked : GeneralEvents()
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