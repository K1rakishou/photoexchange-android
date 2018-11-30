package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto

sealed class ReceivedPhotosFragmentEvent : BaseEvent {
  sealed class GeneralEvents : ReceivedPhotosFragmentEvent() {
    class ScrollToTop : GeneralEvents()
    object FetchFreshPhotos : GeneralEvents()
  }

  sealed class ReceivePhotosEvent : ReceivedPhotosFragmentEvent() {
    class PhotosReceived(val receivedPhotos: List<ReceivedPhoto>) : ReceivePhotosEvent()
    class NoPhotosReceived : ReceivePhotosEvent()
    class OnFailed(val error: Throwable) : ReceivePhotosEvent()
  }
}