package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode
import com.kirakishou.photoexchange.mvvm.model.exception.ApiException
import com.kirakishou.photoexchange.mvvm.model.net.response.StatusResponse
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
                Timber.e("Unknown exception")
                throw error
            }
        }

        return response as T
    }
}