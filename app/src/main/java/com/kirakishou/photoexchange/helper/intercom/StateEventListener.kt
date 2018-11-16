package com.kirakishou.photoexchange.helper.intercom

interface StateEventListener<T> {
  fun onStateEvent(event: T)
}