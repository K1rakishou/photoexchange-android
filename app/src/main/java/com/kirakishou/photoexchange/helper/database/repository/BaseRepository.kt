package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

abstract class BaseRepository(
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.DB()
}