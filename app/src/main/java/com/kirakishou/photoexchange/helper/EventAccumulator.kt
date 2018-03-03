package com.kirakishou.photoexchange.helper

import java.util.*
import kotlin.NoSuchElementException

/**
 * Created by kirakishou on 12/20/2017.
 */
class EventAccumulator {
    /*private val eventMap = hashMapOf<Class<*>, Queue<BaseEvent>>()

    fun rememberEvent(clazz: Class<*>, event: BaseEvent) {
        synchronized(eventMap) {
            if (!eventMap.containsKey(clazz)) {
                eventMap.put(clazz, LinkedList())
            }

            eventMap[clazz]!!.add(event)
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

            return@synchronized eventMap[clazz]!!.poll()
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

    fun eventsCount(clazz: Class<*>): Int {
        return synchronized(eventMap) {
            if (!eventMap.containsKey(clazz)) {
                return@synchronized 0
            }

            return@synchronized eventMap[clazz]!!.size
        }
    }

    fun clear() {
        eventMap.clear()
    }*/
}