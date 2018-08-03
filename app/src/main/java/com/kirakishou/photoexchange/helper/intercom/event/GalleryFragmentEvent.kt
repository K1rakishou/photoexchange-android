package com.kirakishou.photoexchange.helper.intercom.event

sealed class GalleryFragmentEvent : BaseEvent {
    sealed class GeneralEvents : GalleryFragmentEvent() {
        class ShowProgressFooter : GeneralEvents()
        class HideProgressFooter : GeneralEvents()
        class OnPageSelected : GeneralEvents()
    }
}