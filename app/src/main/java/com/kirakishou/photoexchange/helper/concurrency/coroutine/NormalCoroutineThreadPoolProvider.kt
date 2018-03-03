package com.kirakishou.photoexchange.helper.concurrency.coroutine

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.ThreadPoolDispatcher
import kotlinx.coroutines.experimental.android.HandlerContext
import kotlinx.coroutines.experimental.newSingleThreadContext

/**
 * Created by kirakishou on 3/3/2018.
 */
class NormalCoroutineThreadPoolProvider : CoroutineThreadPoolProvider {

    override fun provideCommon(): CoroutineDispatcher = newSingleThreadContext("Network")
    override fun provideDb(): CoroutineDispatcher = newSingleThreadContext("Database")
    override fun provideNetwork(): ThreadPoolDispatcher = newSingleThreadContext("Common")
    override fun provideMain(): CoroutineDispatcher = HandlerContext(Handler(Looper.getMainLooper()), "UI")
}