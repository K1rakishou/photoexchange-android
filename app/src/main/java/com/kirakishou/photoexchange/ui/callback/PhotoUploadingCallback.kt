package com.kirakishou.photoexchange.ui.callback

import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent


/**
 * Created by kirakishou on 3/17/2018.
 */
interface PhotoUploadingCallback {
    fun onUploadingEvent(event: PhotoUploadingEvent)
}