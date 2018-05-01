package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.mvp.model.PhotoAnswer
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

interface FindPhotoAnswerServiceCallbacks {
    fun onPhotoReceived(photoAnswer: PhotoAnswer, photoId: Long)
    fun onFailed(errorCode: ErrorCode.GetPhotoAnswersErrors)
    fun onError(error: Throwable)
    fun stopService()
}