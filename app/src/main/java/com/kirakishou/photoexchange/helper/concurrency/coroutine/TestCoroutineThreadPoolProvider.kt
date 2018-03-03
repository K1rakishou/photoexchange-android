package com.kirakishou.photoexchange.helper.concurrency.coroutine

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.newSingleThreadContext

/**
 * Created by kirakishou on 3/3/2018.
 */
class TestCoroutineThreadPoolProvider : CoroutineThreadPoolProvider {
    override fun provideIo(): CoroutineDispatcher {
        return newSingleThreadContext("TEST")
    }

    override fun provideMain(): CoroutineDispatcher {
        return newSingleThreadContext("TEST")
    }
}