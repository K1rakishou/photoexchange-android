package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvrx.model.NewReceivedPhoto
import com.kirakishou.photoexchange.mvrx.model.photo.ReceivedPhoto

sealed class ReceivedPhotosFragmentEvent : BaseEvent {
  sealed class GeneralEvents : ReceivedPhotosFragmentEvent() {
    class ScrollToTop : GeneralEvents()
    class OnNewPhotosReceived(val newReceivedPhotos: List<NewReceivedPhoto>) : GeneralEvents()
    class RemovePhoto(val photoName: String) : GeneralEvents()
    class PhotoReported(val photoName: String,
                        val isReported: Boolean) : GeneralEvents()
    class PhotoFavourited(val photoName: String,
                          val isFavourited: Boolean,
                          val favouritesCount: Long) : GeneralEvents()
    object OnTabSelected : GeneralEvents()
  }

  sealed class ReceivePhotosEvent : ReceivedPhotosFragmentEvent() {
    class PhotosReceived(val receivedPhotos: List<ReceivedPhoto>) : ReceivePhotosEvent()
    class OnFailed(val error: Throwable) : ReceivePhotosEvent()
  }
}