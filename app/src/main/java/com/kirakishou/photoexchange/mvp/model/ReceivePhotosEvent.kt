package com.kirakishou.photoexchange.mvp.model

import com.kirakishou.photoexchange.helper.intercom.event.BaseEvent
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class ReceivePhotosEvent : BaseEvent {
    class OnPhotoReceived(val receivedPhoto: ReceivedPhoto,
                          val takenPhotoId: Long) : ReceivePhotosEvent()
    class OnFailed(val errorCode: ErrorCode) : ReceivePhotosEvent()
    class OnUnknownError(val error: Throwable) : ReceivePhotosEvent()
}