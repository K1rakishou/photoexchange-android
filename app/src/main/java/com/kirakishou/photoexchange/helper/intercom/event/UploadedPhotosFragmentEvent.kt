package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import java.lang.Exception

sealed class UploadedPhotosFragmentEvent : BaseEvent {
  sealed class GeneralEvents : UploadedPhotosFragmentEvent() {
    class UpdateReceiverInfo(val receivedPhotos: List<ReceivedPhoto>) : GeneralEvents()
    class OnPageSelected : GeneralEvents()
    class PhotoReceived(val takenPhotoName: String) : GeneralEvents()
    object Invalidate : GeneralEvents()
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