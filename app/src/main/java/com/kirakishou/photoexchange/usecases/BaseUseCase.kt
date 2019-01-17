package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

abstract class BaseUseCase(
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()
}