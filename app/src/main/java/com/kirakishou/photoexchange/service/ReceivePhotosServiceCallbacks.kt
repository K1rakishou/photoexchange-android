package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

interface ReceivePhotosServiceCallbacks {
    fun onPhotoReceived(receivedPhoto: ReceivedPhoto, takenPhotoId: Long)
    fun onFailed(errorCode: ErrorCode.ReceivePhotosErrors)
    fun onError(error: Throwable)
    fun stopService()
}