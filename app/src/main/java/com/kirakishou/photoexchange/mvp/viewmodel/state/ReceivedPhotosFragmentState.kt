package com.kirakishou.photoexchange.mvp.viewmodel.state

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto

data class ReceivedPhotosFragmentState(
  val favouritedPhotos: Set<String> = hashSetOf(),
  val reportedPhotos: Set<String> = hashSetOf(),

  val isEndReached: Boolean = false,
  val receivedPhotos: List<ReceivedPhoto> = emptyList(),
  val receivedPhotosRequest: Async<Paged<ReceivedPhoto>> = Uninitialized
) : MvRxState