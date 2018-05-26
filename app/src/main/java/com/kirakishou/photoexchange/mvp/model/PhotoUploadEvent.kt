package com.kirakishou.photoexchange.mvp.model

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

/**
 * Created by kirakishou on 3/17/2018.
 */
sealed class PhotoUploadEvent {
    class OnLocationUpdateStart : PhotoUploadEvent()
    class OnLocationUpdateEnd : PhotoUploadEvent()
    class OnFailedToUpload(val photo: TakenPhoto, val errorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadEvent()
    class OnUnknownError(val error: Throwable) : PhotoUploadEvent()
    class OnCouldNotGetUserIdFromServerError(val errorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadEvent()
    class OnPrepare : PhotoUploadEvent()
    class OnPhotoUploadStart(val photo: TakenPhoto) : PhotoUploadEvent()
    class OnProgress(val photo: TakenPhoto, val progress: Int) : PhotoUploadEvent()
    class OnUploaded(val photo: UploadedPhoto) : PhotoUploadEvent()
    class OnFoundPhotoAnswer(val takenPhotoId: Long) : PhotoUploadEvent()
    class OnEnd(val allUploaded: Boolean) : PhotoUploadEvent()
}