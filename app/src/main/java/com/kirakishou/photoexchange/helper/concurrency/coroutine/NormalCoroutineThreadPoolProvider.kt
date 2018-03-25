package com.kirakishou.photoexchange.helper.concurrency.coroutine

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.newSingleThreadContext

/**
 * Created by kirakishou on 3/3/2018.
 */
class NormalCoroutineThreadPoolProvider : CoroutineThreadPoolProvider {
    override fun BG(): CoroutineDispatcher = newFixedThreadPoolContext(getThreadsCount(), "BG")
    override fun UI(): CoroutineDispatcher = newSingleThreadContext("UI")

    private fun getThreadsCount(): Int {
        val count = Runtime.getRuntime().availableProcessors() - 1
        if (count <= 0) {
            return 1
        }

        return count
    }
}