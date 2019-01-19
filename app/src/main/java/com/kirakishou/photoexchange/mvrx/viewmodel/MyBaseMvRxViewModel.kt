package com.kirakishou.photoexchange.mvrx.viewmodel

import com.airbnb.mvrx.*
import kotlinx.coroutines.runBlocking

abstract class MyBaseMvRxViewModel<S : MvRxState>(
  initialState: S,
  private val debugMode: Boolean = false,
  private val stateStore: MvRxStateStore<S> = RealMvRxStateStore(initialState)
) : BaseMvRxViewModel<S>(initialState) {

  suspend fun suspendWithState(block: suspend (state: S) -> Unit) {
    withState { state ->
      runBlocking {
        block(state)
      }
    }
  }

}