package com.kirakishou.photoexchange.helper.concurrency.coroutine

import kotlinx.coroutines.experimental.CoroutineDispatcher

/**
 * Created by kirakishou on 3/3/2018.
 */
interface CoroutineThreadPoolProvider {
    fun BG(): CoroutineDispatcher
    fun UI(): CoroutineDispatcher
}