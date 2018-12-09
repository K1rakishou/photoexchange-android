package com.kirakishou.photoexchange.helper.intercom.event

sealed class GalleryFragmentEvent : BaseEvent {
  sealed class GeneralEvents : GalleryFragmentEvent() {
    object ScrollToTop : GeneralEvents()
    class RemovePhoto(val photoName: String) : GeneralEvents()
  }
}