package com.kirakishou.photoexchange.mvp.model

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class PhotoFindEvent {
    class OnPhotoAnswerFound(val photoAnswer: PhotoAnswer, val photoId: Long) : PhotoFindEvent()
    class OnFailed(val errorCode: ErrorCode) : PhotoFindEvent()
    class OnUnknownError(val error: Throwable) : PhotoFindEvent()
}