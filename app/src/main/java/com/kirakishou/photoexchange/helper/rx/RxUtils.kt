package com.kirakishou.photoexchange.helper.rx

import io.reactivex.Observable
import io.reactivex.functions.Predicate
import timber.log.Timber

/**
 * Created by kirakishou on 9/16/2017.
 */
object RxUtils {

    suspend fun <Argument, Result> repeatRequest(maxAttempts: Int, arg: Argument, block: suspend (arg: Argument) -> Result): Result? {
        var attempt = maxAttempts
        var response: Result? = null

        while (attempt-- > 0) {
            try {
                Timber.d("Trying to send request, attempt #${maxAttempts - attempt}")
                response = block(arg)
                return response!!
            } catch (error: Throwable) {
                Timber.e(error)
            }
        }

        return null
    }
}