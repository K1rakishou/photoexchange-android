package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import java.lang.Exception

sealed class UploadedPhotosFragmentEvent : BaseEvent {
  sealed class GeneralEvents : UploadedPhotosFragmentEvent() {
    class OnPageSelected : GeneralEvents()
  }

  sealed class ReceivePhotosEvent : UploadedPhotosFragmentEvent() {
    class PhotosReceived(val receivedPhotos: List<ReceivedPhoto>) : ReceivePhotosEvent()
    class NoPhotosReceived : ReceivePhotosEvent()
    class OnFailed(val error: Throwable) : ReceivePhotosEvent()
  }

  sealed class PhotoUploadEvent : UploadedPhotosFragmentEvent() {
    class OnPhotoUploadingStart(val photo: TakenPhoto) : PhotoUploadEvent()
    class OnPhotoUploadingProgress(val photo: TakenPhoto, val progress: Int) : PhotoUploadEvent()
    class OnPhotoUploaded(val photo: TakenPhoto,
                          val newPhotoId: Long,
                          val newPhotoName: String,
                          val uploadedOn: Long,
                          val currentLocation: LonLat) : PhotoUploadEvent()
    class OnFailedToUploadPhoto(val photo: TakenPhoto) : PhotoUploadEvent()
    class OnPhotoCanceled(val photo: TakenPhoto) : PhotoUploadEvent()
    class OnError(val exception: Exception) : PhotoUploadEvent()
    class OnEnd : PhotoUploadEvent()
  }
}