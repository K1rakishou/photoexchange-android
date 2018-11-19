package com.kirakishou.photoexchange.helper.intercom

interface StateEventListener<T> {
  suspend fun onStateEvent(event: T)
}