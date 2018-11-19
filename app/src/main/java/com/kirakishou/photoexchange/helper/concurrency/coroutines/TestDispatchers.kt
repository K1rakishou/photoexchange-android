package com.kirakishou.photoexchange.helper.concurrency.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class TestDispatchers : DispatchersProvider {
  override fun IO(): CoroutineDispatcher = Dispatchers.Unconfined
  override fun GENERAL(): CoroutineDispatcher = Dispatchers.Unconfined
  override fun UI(): CoroutineDispatcher = Dispatchers.Unconfined
}