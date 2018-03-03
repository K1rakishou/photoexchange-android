package com.kirakishou.photoexchange.helper.concurrency.coroutine

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.ThreadPoolDispatcher
import kotlinx.coroutines.experimental.android.HandlerContext
import kotlinx.coroutines.experimental.newFixedThreadPoolContext

/**
 * Created by kirakishou on 3/3/2018.
 */
class NormalCoroutineThreadPoolProvider : CoroutineThreadPoolProvider {
    override fun provideIo(): ThreadPoolDispatcher {
        return newFixedThreadPoolContext(2, "Common")
    }

    override fun provideMain(): CoroutineDispatcher {
        return HandlerContext(Handler(Looper.getMainLooper()), "UI")
    }
}