package com.kirakishou.photoexchange.helper.concurrency.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newFixedThreadPoolContext

class NormalDispatchers : DispatchersProvider {
  override fun IO(): CoroutineDispatcher = Dispatchers.IO
  override fun GENERAL(): CoroutineDispatcher = Dispatchers.Default
  override fun UI(): CoroutineDispatcher = Dispatchers.Main
}