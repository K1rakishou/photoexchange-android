package com.kirakishou.photoexchange.mvp.model

/**
 * Created by kirakishou on 3/17/2018.
 */
sealed class PhotoUploadingEvent {
    class onPrepare() : PhotoUploadingEvent()
    class onPhotoUploadingStart(val photoId: Long) : PhotoUploadingEvent()
    class onProgress(val photoId: Long, val progress: Int) : PhotoUploadingEvent()
    class onUploaded(val photoId: Long) : PhotoUploadingEvent()
    class onFailedToUpload(val photoId: Long) : PhotoUploadingEvent()
    class onUnknownError() : PhotoUploadingEvent()
    class onEnd() : PhotoUploadingEvent()
}