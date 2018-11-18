package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Created by kirakishou on 3/17/2018.
 */
abstract class AbstractRequest<T>(
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.IO()

  abstract suspend fun execute(): T
}