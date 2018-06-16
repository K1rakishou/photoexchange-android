package com.kirakishou.photoexchange.helper.intercom.event

sealed class GalleryFragmentEvent : BaseEvent {
    sealed class UiEvents : GalleryFragmentEvent() {
        class ShowProgressFooter : UiEvents()
        class HideProgressFooter : UiEvents()
    }
}