package com.kirakishou.photoexchange.helper.intercom.event

sealed class GalleryFragmentEvent : BaseEvent {
  sealed class GeneralEvents : GalleryFragmentEvent() {
    class ShowToast(val message: String) : GeneralEvents()
  }
}