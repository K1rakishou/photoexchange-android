package com.kirakishou.photoexchange.mvp.viewmodel.state

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto

data class ReceivedPhotosFragmentState(
  val isEndReached: Boolean = false,

  val receivedPhotos: List<ReceivedPhoto> = emptyList(),
  val receivedPhotosRequest: Async<List<ReceivedPhoto>> = Uninitialized
) : MvRxState