package com.kirakishou.photoexchange.helper

import com.kirakishou.photoexchange.mwvm.model.event.BaseEvent
import java.util.*

/**
 * Created by kirakishou on 12/20/2017.
 */
class EventAccumulator {
    private val eventMap = hashMapOf<Class<*>, LinkedList<BaseEvent>>()

    fun rememberEvent(clazz: Class<*>, event: BaseEvent) {
        synchronized(eventMap) {
            if (eventMap[clazz]!!.isEmpty()) {
                eventMap[clazz]!!.push(event)
            }
        }
    }

    fun getEvent(clazz: Class<*>): BaseEvent {
        return synchronized(eventMap) {
            return@synchronized eventMap[clazz]!!.pop()
        }
    }

    fun hasEvent(clazz: Class<*>): Boolean {
        return synchronized(eventMap) {
            if (!eventMap.containsKey(clazz)) {
                return@synchronized false
            }

            if (eventMap[clazz]!!.isEmpty()) {
                return@synchronized false
            }

            return@synchronized true
        }
    }
}