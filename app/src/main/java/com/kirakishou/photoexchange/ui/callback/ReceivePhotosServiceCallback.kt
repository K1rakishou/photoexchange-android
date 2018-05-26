package com.kirakishou.photoexchange.ui.callback

import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent

interface ReceivePhotosServiceCallback {
    fun onPhotoFindEvent(event: ReceivedPhotosFragmentEvent.ReceivePhotosEvent)
}