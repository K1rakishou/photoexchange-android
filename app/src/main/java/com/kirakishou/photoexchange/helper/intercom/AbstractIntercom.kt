package com.kirakishou.photoexchange.helper.intercom

import io.reactivex.Observable

interface AbstractIntercom<EventType> {
    fun listen(): Observable<EventType>
    fun to(event: EventType)
    fun that(event: EventType)
}