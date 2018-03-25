package com.kirakishou.photoexchange.mvp.model.other

/**
 * Created by kirakishou on 12/28/2017.
 */
class MulticastEvent<out T>(
    val receiver: Class<*>,
    val obj: T?
)