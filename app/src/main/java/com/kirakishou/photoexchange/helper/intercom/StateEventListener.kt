package com.kirakishou.photoexchange.helper.intercom

import com.kirakishou.photoexchange.helper.intercom.event.BaseEvent

interface StateEventListener {
    fun onStateEvent(event: BaseEvent)
}