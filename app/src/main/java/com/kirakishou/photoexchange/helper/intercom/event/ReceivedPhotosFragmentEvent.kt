package com.kirakishou.photoexchange.helper.intercom.event

sealed class ReceivedPhotosFragmentEvent : BaseEvent {
    class ScrollToTop : ReceivedPhotosFragmentEvent()
}