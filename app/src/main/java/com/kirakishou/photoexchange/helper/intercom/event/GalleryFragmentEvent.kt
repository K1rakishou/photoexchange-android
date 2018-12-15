package com.kirakishou.photoexchange.helper.intercom.event

sealed class GalleryFragmentEvent : BaseEvent {
  sealed class GeneralEvents : GalleryFragmentEvent() {
    object ScrollToTop : GeneralEvents()
    class RemovePhoto(val photoName: String) : GeneralEvents()
    class PhotoReported(val photoName: String,
                        val isReported: Boolean) : GeneralEvents()
    class PhotoFavourited(val photoName: String,
                          val isFavourited: Boolean,
                          val favouritesCount: Long) : GeneralEvents()
  }
}