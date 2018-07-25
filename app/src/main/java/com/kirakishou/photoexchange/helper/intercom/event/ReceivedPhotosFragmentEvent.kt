package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class ReceivedPhotosFragmentEvent : BaseEvent {
    sealed class GeneralEvents : ReceivedPhotosFragmentEvent() {
        class ScrollToTop : GeneralEvents()
        class ShowProgressFooter : GeneralEvents()
        class HideProgressFooter : GeneralEvents()
        class StartRefreshing : GeneralEvents()
        class StopRefreshing : GeneralEvents()
        class AddReceivedPhotos(val photos: List<ReceivedPhoto>) : GeneralEvents()
        class ClearAdapter : GeneralEvents()
        class ClearCache : GeneralEvents()
    }

    sealed class ReceivePhotosEvent : ReceivedPhotosFragmentEvent() {
        class PhotoReceived(val receivedPhoto: ReceivedPhoto,
                            val takenPhotoName: String) : ReceivePhotosEvent()
        class OnFailed(val errorCode: ErrorCode?) : ReceivePhotosEvent()
        class OnUnknownError(val error: Throwable) : ReceivePhotosEvent()
    }
}