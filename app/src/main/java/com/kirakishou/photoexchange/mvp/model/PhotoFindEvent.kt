package com.kirakishou.photoexchange.mvp.model

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class PhotoFindEvent {
    class OnPhotoAnswerFound(photoAnswer: PhotoAnswer) : PhotoFindEvent()
    class OnFailed(errorCode: ErrorCode) : PhotoFindEvent()
    class OnUnknownError(error: Throwable) : PhotoFindEvent()
}