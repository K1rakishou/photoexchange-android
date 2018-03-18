package com.kirakishou.photoexchange.mvp.model

/**
 * Created by kirakishou on 3/17/2018.
 */
sealed class PhotoUploadingEvent {
    class onPrepare() : PhotoUploadingEvent()
    class onPhotoUploadingStart(val myPhoto: MyPhoto) : PhotoUploadingEvent()
    class onProgress(val photoId: Long, val progress: Int) : PhotoUploadingEvent()
    class onUploaded(val myPhoto: MyPhoto) : PhotoUploadingEvent()
    class onFailedToUpload(val myPhoto: MyPhoto) : PhotoUploadingEvent()
    class onUnknownError() : PhotoUploadingEvent()
    class onEnd() : PhotoUploadingEvent()
}