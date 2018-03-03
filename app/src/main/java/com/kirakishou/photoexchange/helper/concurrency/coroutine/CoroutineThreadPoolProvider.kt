package com.kirakishou.photoexchange.helper.concurrency.coroutine

import kotlinx.coroutines.experimental.CoroutineDispatcher

/**
 * Created by kirakishou on 3/3/2018.
 */
interface CoroutineThreadPoolProvider {
    fun provideNetwork(): CoroutineDispatcher
    fun provideDb(): CoroutineDispatcher
    fun provideCommon(): CoroutineDispatcher
    fun provideMain(): CoroutineDispatcher
}