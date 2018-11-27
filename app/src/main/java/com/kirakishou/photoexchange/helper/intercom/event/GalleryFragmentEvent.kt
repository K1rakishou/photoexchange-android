package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto

sealed class GalleryFragmentEvent : BaseEvent {
  sealed class GeneralEvents : GalleryFragmentEvent() {
    class ShowProgressFooter : GeneralEvents()
    class HideProgressFooter : GeneralEvents()
    class PageIsLoading : GeneralEvents()
    class ShowGalleryPhotos(val photos: List<GalleryPhoto>) : GeneralEvents()
  }
}