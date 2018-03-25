package com.kirakishou.photoexchange.helper.api.request

import io.reactivex.Single

/**
 * Created by kirakishou on 3/17/2018.
 */
abstract class AbstractRequest<T> {
    abstract fun execute(): Single<T>
}