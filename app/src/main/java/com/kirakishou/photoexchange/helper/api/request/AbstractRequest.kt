package com.kirakishou.photoexchange.helper.api.request

import kotlinx.coroutines.experimental.Deferred

/**
 * Created by kirakishou on 3/17/2018.
 */
abstract class AbstractRequest<T> {
    abstract fun execute(): Deferred<T>
}