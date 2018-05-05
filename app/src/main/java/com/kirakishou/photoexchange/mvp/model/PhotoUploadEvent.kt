package com.kirakishou.photoexchange.mvp.model

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

/**
 * Created by kirakishou on 3/17/2018.
 */
sealed class PhotoUploadEvent {
    class OnFailedToUpload(val photo: MyPhoto, val errorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadEvent()
    class OnUnknownError(val error: Throwable) : PhotoUploadEvent()
    class OnPrepare : PhotoUploadEvent()
    class OnPhotoUploadStart(val photo: MyPhoto) : PhotoUploadEvent()
    class OnProgress(val photo: MyPhoto, val progress: Int) : PhotoUploadEvent()
    class OnUploaded(val photo: MyPhoto) : PhotoUploadEvent()
    class OnFoundPhotoAnswer(val photoId: Long) : PhotoUploadEvent()
    class OnEnd(val allUploaded: Boolean) : PhotoUploadEvent()
}