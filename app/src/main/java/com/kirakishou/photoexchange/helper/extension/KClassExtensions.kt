package com.kirakishou.photoexchange.helper.extension

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * Created by kirakishou on 11/7/2017.
 */

fun <T> KClass<*>.newInstance(): T {
    val ctor = this.primaryConstructor
    return if (ctor != null && ctor.parameters.isEmpty()) {
        ctor.call() as T
    } else {
        throw IllegalStateException("ctor is null or empty")
    }
}