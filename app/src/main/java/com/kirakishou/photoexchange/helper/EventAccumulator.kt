package com.kirakishou.photoexchange.helper

import com.kirakishou.photoexchange.mwvm.model.event.BaseEvent
import java.util.*
import kotlin.NoSuchElementException

/**
 * Created by kirakishou on 12/20/2017.
 */
class EventAccumulator {
    private val eventMap = hashMapOf<Class<*>, LinkedList<BaseEvent>>()

    fun rememberEvent(clazz: Class<*>, event: BaseEvent) {
        synchronized(eventMap) {
            if (eventMap[clazz]!!.isEmpty()) {
                eventMap[clazz] = LinkedList()
            }

            eventMap[clazz]!!.push(event)
        }
    }

    fun getEvent(clazz: Class<*>): BaseEvent {
        return synchronized(eventMap) {
            if (!eventMap.containsKey(clazz))  {
                throw NoSuchElementException("eventMap does not contain $clazz key")
            }

            if (eventMap[clazz]!!.isEmpty()) {
                throw IllegalStateException("eventMap does not have any events with key $clazz")
            }

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