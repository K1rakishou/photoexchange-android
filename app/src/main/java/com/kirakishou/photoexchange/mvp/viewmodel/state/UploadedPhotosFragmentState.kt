package com.kirakishou.photoexchange.mvp.viewmodel.state

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto

data class UploadedPhotosFragmentState(
  val takenPhotos: List<TakenPhoto> = emptyList(),
  val takenPhotosRequest: Async<List<TakenPhoto>> = Uninitialized,

  val uploadedPhotos: List<UploadedPhoto> = emptyList(),
  val uploadedPhotosRequest: Async<List<UploadedPhoto>> = Uninitialized
) : MvRxState