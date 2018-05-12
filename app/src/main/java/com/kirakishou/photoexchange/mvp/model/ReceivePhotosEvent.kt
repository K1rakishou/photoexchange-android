package com.kirakishou.photoexchange.mvp.model

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class ReceivePhotosEvent {
    class OnPhotoReceived(val photoAnswer: PhotoAnswer, val photoId: Long) : ReceivePhotosEvent()
    class OnFailed(val errorCode: ErrorCode) : ReceivePhotosEvent()
    class OnUnknownError(val error: Throwable) : ReceivePhotosEvent()
}