package com.kirakishou.photoexchange.interactors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

abstract class BaseUseCase : CoroutineScope {
  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job

}