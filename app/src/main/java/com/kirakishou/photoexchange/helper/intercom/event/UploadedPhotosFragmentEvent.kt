package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import java.lang.Exception

sealed class UploadedPhotosFragmentEvent : BaseEvent {
  sealed class GeneralEvents : UploadedPhotosFragmentEvent() {
    class RemovePhoto(val photo: TakenPhoto) : GeneralEvents()
    class AddPhoto(val photo: TakenPhoto) : GeneralEvents()
    class ScrollToTop : GeneralEvents()
    class PhotoRemoved : GeneralEvents()
    class AfterPermissionRequest : GeneralEvents()
    class UpdateReceiverInfo(val receivedPhotos: List<ReceivedPhoto>) : GeneralEvents()
    class OnPageSelected : GeneralEvents()
    class ShowTakenPhotos(val takenPhotos: List<TakenPhoto>) : GeneralEvents()
    class ShowUploadedPhotos(val uploadedPhotos: List<UploadedPhoto>) : GeneralEvents()
    class PhotoReceived(val takenPhotoName: String) : GeneralEvents()
  }

  sealed class PhotoUploadEvent : UploadedPhotosFragmentEvent() {
    class OnPhotoUploadingStart(val photo: TakenPhoto) : PhotoUploadEvent()
    class OnPhotoUploadingProgress(val photo: TakenPhoto, val progress: Int) : PhotoUploadEvent()
    class OnPhotoUploaded(val photo: TakenPhoto) : PhotoUploadEvent()
    class OnFailedToUploadPhoto(val photo: TakenPhoto) : PhotoUploadEvent()
    class OnEnd : PhotoUploadEvent()
    class OnError(val exception: Exception) : PhotoUploadEvent()

  }
}