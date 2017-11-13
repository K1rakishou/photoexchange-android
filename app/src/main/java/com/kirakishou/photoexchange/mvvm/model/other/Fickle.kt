package com.kirakishou.photoexchange.mvvm.model.other

/**
 * Created by kirakishou on 7/21/2017.
 */

class Fickle<out T>(private var value: T? = null) {

    private constructor() : this(null)

    fun isPresent(): Boolean {
        return value != null
    }

    fun get(): T {
        return value!!
    }

    fun ifPresent(func: (v: T) -> Unit) {
        value?.let {
            func.invoke(it)
        }
    }

    companion object {
        fun <T> empty(): Fickle<T> {
            return Fickle()
        }

        fun <T> of(value: T?): Fickle<T> {
            if (value == null) {
                return empty()
            }

            return Fickle(value)
        }
    }
}