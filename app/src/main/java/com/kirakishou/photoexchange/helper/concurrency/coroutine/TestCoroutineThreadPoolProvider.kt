package com.kirakishou.photoexchange.helper.concurrency.coroutine

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.newSingleThreadContext

/**
 * Created by kirakishou on 3/3/2018.
 */
class TestCoroutineThreadPoolProvider : CoroutineThreadPoolProvider {
    val threadContext = newSingleThreadContext("TEST")

    override fun provideCommon(): CoroutineDispatcher = threadContext
    override fun provideDb(): CoroutineDispatcher = threadContext
    override fun provideNetwork(): CoroutineDispatcher = threadContext
    override fun provideMain(): CoroutineDispatcher = threadContext
}