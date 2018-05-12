package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.mvp.model.PhotoAnswer
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

interface ReceivePhotosServiceCallbacks {
    fun onPhotoReceived(photoAnswer: PhotoAnswer, photoId: Long)
    fun onFailed(errorCode: ErrorCode.ReceivePhotosErrors)
    fun onError(error: Throwable)
    fun stopService()
}