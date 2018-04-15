package com.kirakishou.photoexchange.mvp.model

/**
 * Created by kirakishou on 3/17/2018.
 */
sealed class PhotoUploadingEvent {
    class OnFailedToUpload(val myPhoto: MyPhoto) : PhotoUploadingEvent()
    class OnUnknownError : PhotoUploadingEvent()
    class OnPrepare : PhotoUploadingEvent()
    class OnPhotoUploadingStart(val myPhoto: MyPhoto) : PhotoUploadingEvent()
    class OnProgress(val photo: MyPhoto, val progress: Int) : PhotoUploadingEvent()
    class OnUploaded(val myPhoto: MyPhoto) : PhotoUploadingEvent()
    class OnEnd : PhotoUploadingEvent()
}