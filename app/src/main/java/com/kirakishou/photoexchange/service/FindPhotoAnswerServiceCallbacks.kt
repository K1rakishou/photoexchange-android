package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.mvp.model.PhotoAnswer
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

interface FindPhotoAnswerServiceCallbacks {
    fun onPhotoReceived(photoAnswer: PhotoAnswer)
    fun onFailed(errorCode: ErrorCode.FindPhotoAnswerErrors)
    fun onError(error: Throwable)
    fun stopService()
}