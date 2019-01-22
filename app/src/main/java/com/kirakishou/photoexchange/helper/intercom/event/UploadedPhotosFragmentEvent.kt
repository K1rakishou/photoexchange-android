package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvrx.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.mvrx.model.photo.TakenPhoto
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.mvrx.model.NewReceivedPhoto
import java.lang.Exception

sealed class UploadedPhotosFragmentEvent : BaseEvent {
  sealed class GeneralEvents : UploadedPhotosFragmentEvent() {
    class OnNewPhotosReceived(val newReceivedPhotos: List<NewReceivedPhoto>) : GeneralEvents()
    object OnTabSelected : GeneralEvents()
    object ReloadAllPhotos : GeneralEvents()
    class ScrollToTop : GeneralEvents()
  }

  sealed class ReceivePhotosEvent : UploadedPhotosFragmentEvent() {
    class OnPhotosReceived(val receivedPhotos: List<ReceivedPhoto>) : ReceivePhotosEvent()
    class OnFailed(val error: Throwable) : ReceivePhotosEvent()
  }

  sealed class PhotoUploadEvent : UploadedPhotosFragmentEvent() {
    class OnPhotoUploadingStart : PhotoUploadEvent()
    class OnPhotoUploadingProgress(val photo: TakenPhoto, val progress: Int) : PhotoUploadEvent()
    class OnPhotoUploaded(val photo: TakenPhoto,
                          val newPhotoId: Long,
                          val newPhotoName: String,
                          val uploadedOn: Long,
                          val currentLocation: LonLat) : PhotoUploadEvent()
    class OnFailedToUploadPhoto(val photo: TakenPhoto,
                                val error: Throwable) : PhotoUploadEvent()
    class OnPhotoCanceled(val photo: TakenPhoto) : PhotoUploadEvent()
    class OnError(val exception: Exception) : PhotoUploadEvent()
    class OnEnd : PhotoUploadEvent()
  }
}