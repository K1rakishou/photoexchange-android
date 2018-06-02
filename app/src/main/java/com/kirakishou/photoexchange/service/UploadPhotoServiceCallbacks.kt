package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Single

/**
 * Created by kirakishou on 3/17/2018.
 */
interface UploadPhotoServiceCallbacks {
    fun getCurrentLocation(): Single<LonLat>
    fun onUploadingEvent(event: UploadedPhotosFragmentEvent.PhotoUploadEvent)
    fun onError(error: Throwable)
    fun stopService()

    fun updateUploadingNotificationShowUploading()
    fun updateUploadingNotificationShowSuccess(message: String)
    fun updateUploadingNotificationShowError(message: String)
}