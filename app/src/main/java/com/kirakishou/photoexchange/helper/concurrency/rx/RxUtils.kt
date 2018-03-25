package com.kirakishou.photoexchange.helper.concurrency.rx

import kotlinx.coroutines.experimental.async
import timber.log.Timber

/**
 * Created by kirakishou on 9/16/2017.
 */
object RxUtils {

    suspend fun <Argument, Result> repeatRequest(maxAttempts: Int, arg: Argument, block: suspend (arg: Argument) -> Result): Result? {
        var attempt = maxAttempts

        return async {
            while (attempt-- > 0 && isActive) {
                try {
                    Timber.d("Trying to send request, attempt #${maxAttempts - attempt}")
                    return@async block(arg)
                } catch (error: Throwable) {
                    Timber.e(error)
                }
            }

            return@async null
        }.await()
    }
}