package com.kirakishou.photoexchange.mvp.viewmodel.state

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto

data class UploadedPhotosFragmentState(
  val shouldShowNoPhotosMessage: Boolean = false,
  val takenPhotos: List<TakenPhoto> = emptyList(),

  val uploadedPhotos: List<UploadedPhoto> = emptyList(),
  val uploadedPhotosRequest: Async<List<UploadedPhoto>> = Uninitialized
) : MvRxState