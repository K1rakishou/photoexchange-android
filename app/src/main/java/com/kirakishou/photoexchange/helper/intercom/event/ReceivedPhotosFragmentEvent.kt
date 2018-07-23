package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class ReceivedPhotosFragmentEvent : BaseEvent {
    sealed class UiEvents : ReceivedPhotosFragmentEvent() {
        class ScrollToTop : UiEvents()
        class ShowProgressFooter : UiEvents()
        class HideProgressFooter : UiEvents()
    }

    sealed class ReceivePhotosEvent : ReceivedPhotosFragmentEvent() {
        class PhotoReceived(val receivedPhoto: ReceivedPhoto,
                            val takenPhotoName: String) : ReceivePhotosEvent()
        class OnFailed(val errorCode: ErrorCode?) : ReceivePhotosEvent()
        class OnUnknownError(val error: Throwable) : ReceivePhotosEvent()
    }
}