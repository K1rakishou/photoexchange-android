package com.kirakishou.photoexchange.helper.extension

import com.kirakishou.photoexchange.mvp.model.other.MulticastEvent
import io.reactivex.Observable

/**
 * Created by kirakishou on 1/3/2018.
 */

fun <T> Observable<MulticastEvent<T>>.filterMulticastEvent(receiver: Class<*>): Observable<T> {
    return this
            .filter { it.receiver == receiver }
            .map { it.obj!! }
}