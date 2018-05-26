package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class ReceivedPhotosFragmentEvent : BaseEvent {
    sealed class UiEvents : ReceivedPhotosFragmentEvent() {
        class ScrollToTop : UiEvents()
    }

    sealed class ReceivePhotosEvent : ReceivedPhotosFragmentEvent() {
        class OnPhotoReceived(val receivedPhoto: ReceivedPhoto,
                              val takenPhotoName: String) : ReceivePhotosEvent()
        class OnFailed(val errorCode: ErrorCode) : ReceivePhotosEvent()
        class OnUnknownError(val error: Throwable) : ReceivePhotosEvent()
    }
}