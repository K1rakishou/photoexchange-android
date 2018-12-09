package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto

sealed class ReceivedPhotosFragmentEvent : BaseEvent {
  sealed class GeneralEvents : ReceivedPhotosFragmentEvent() {
    class ScrollToTop : GeneralEvents()
    class OnNewPhotoNotificationReceived(val photoExchangedData: PhotoExchangedData) : GeneralEvents()
    class RemovePhoto(val photoName: String) : GeneralEvents()
  }

  sealed class ReceivePhotosEvent : ReceivedPhotosFragmentEvent() {
    class PhotosReceived(val receivedPhotos: List<ReceivedPhoto>) : ReceivePhotosEvent()
    class NoPhotosReceived : ReceivePhotosEvent()
    class OnFailed(val error: Throwable) : ReceivePhotosEvent()
  }
}