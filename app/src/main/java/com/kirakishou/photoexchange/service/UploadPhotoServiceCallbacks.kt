package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent

/**
 * Created by kirakishou on 3/17/2018.
 */
interface UploadPhotoServiceCallbacks {
    fun onUploadingEvent(event: PhotoUploadingEvent)
    fun stopService()
}