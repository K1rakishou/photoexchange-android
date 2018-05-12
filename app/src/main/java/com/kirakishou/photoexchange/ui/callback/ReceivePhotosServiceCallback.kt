package com.kirakishou.photoexchange.ui.callback

import com.kirakishou.photoexchange.mvp.model.ReceivePhotosEvent

interface ReceivePhotosServiceCallback {
    fun onPhotoFindEvent(event: ReceivePhotosEvent)
}