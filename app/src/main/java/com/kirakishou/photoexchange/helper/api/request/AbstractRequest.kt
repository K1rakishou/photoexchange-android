package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.mwvm.model.exception.ApiException
import com.kirakishou.photoexchange.mwvm.model.net.response.StatusResponse
import io.reactivex.Single
import timber.log.Timber

/**
 * Created by kirakishou on 11/3/2017.
 */
abstract class AbstractRequest<out T> {
    abstract fun build(): T

    @Suppress("UNCHECKED_CAST")
    protected fun convertExceptionToErrorCode(error: Throwable): T {
        val response = when (error) {
            is ApiException -> StatusResponse(error.serverErrorCode.value)

            else -> {
                Timber.d("Unknown exception")
                throw error
            }
        }

        return Single.just(response) as T
    }
}