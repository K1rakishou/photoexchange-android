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
) : MvRxState {

  fun onPhotoFavourited(
    photoName: String,
    isFavourited: Boolean,
    favouritesCount: Long
  ): UpdateStateResult<List<ReceivedPhoto>> {
    val photoIndex = receivedPhotos.indexOfFirst { it.receivedPhotoName == photoName }
    if (photoIndex == -1) {
      return UpdateStateResult.NothingToUpdate()
    }

    val updatedPhotos = receivedPhotos.toMutableList()
    val receivedPhoto = updatedPhotos[photoIndex]

    val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
      .copy(
        isFavourited = isFavourited,
        favouritesCount = favouritesCount
      )

    updatedPhotos[photoIndex] = receivedPhoto.copy(
      photoAdditionalInfo = updatedPhotoInfo
    )

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun onPhotoReported(
    photoName: String,
    isReported: Boolean
  ): UpdateStateResult<List<ReceivedPhoto>> {
    val photoIndex = receivedPhotos.indexOfFirst { it.receivedPhotoName == photoName }
    if (photoIndex == -1) {
      return UpdateStateResult.NothingToUpdate()
    }

    val updatedPhotos = receivedPhotos.toMutableList()
    val receivedPhoto = updatedPhotos[photoIndex]

    val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
      .copy(isReported = isReported)

    updatedPhotos[photoIndex] = receivedPhoto
      .copy(photoAdditionalInfo = updatedPhotoInfo)

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun reportPhoto(photoName: String, reportResult: Boolean): UpdateStateResult<List<ReceivedPhoto>> {
    val photoIndex = receivedPhotos.indexOfFirst { it.receivedPhotoName == photoName }
    if (photoIndex == -1) {
      return UpdateStateResult.NothingToUpdate()
    }

    val updatedPhotos = receivedPhotos.toMutableList()
    val galleryPhoto = updatedPhotos[photoIndex]

    val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
      .copy(isReported = reportResult)

    updatedPhotos[photoIndex] = galleryPhoto
      .copy(photoAdditionalInfo = updatedPhotoInfo)

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun favouritePhoto(
    photoName: String,
    isFavourited: Boolean,
    favouritesCount: Long
  ): UpdateStateResult<List<ReceivedPhoto>> {
    val photoIndex = receivedPhotos.indexOfFirst { it.receivedPhotoName == photoName }
    if (photoIndex == -1) {
      return UpdateStateResult.NothingToUpdate()
    }

    val updatedPhotos = receivedPhotos.toMutableList()
    val galleryPhoto = updatedPhotos[photoIndex]

    val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
      .copy(isFavourited = isFavourited, favouritesCount = favouritesCount)

    updatedPhotos[photoIndex] = galleryPhoto.copy(photoAdditionalInfo = updatedPhotoInfo)

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun removePhoto(photoName: String): UpdateStateResult<List<ReceivedPhoto>> {
    val photoIndex = receivedPhotos.indexOfFirst { it.receivedPhotoName == photoName }
    if (photoIndex == -1) {
      //nothing to remove
      return UpdateStateResult.NothingToUpdate()
    }

    val updatedPhotos = receivedPhotos.toMutableList().apply {
      removeAt(photoIndex)
    }

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun swapMapAndPhoto(receivedPhotoName: String): UpdateStateResult<List<ReceivedPhoto>> {
    val photoIndex = receivedPhotos.indexOfFirst { receivedPhoto ->
      receivedPhoto.receivedPhotoName == receivedPhotoName
    }

    if (photoIndex == -1) {
      return UpdateStateResult.NothingToUpdate()
    }

    if (receivedPhotos[photoIndex].lonLat.isEmpty()) {
      return UpdateStateResult.SendIntercom()
    }

    val oldShowPhoto = receivedPhotos[photoIndex].showPhoto
    val updatedPhoto = receivedPhotos[photoIndex]
      .copy(showPhoto = !oldShowPhoto)

    val updatedPhotos = receivedPhotos.toMutableList()
    updatedPhotos[photoIndex] = updatedPhoto

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun onPhotosReceived(
    receivedPhotos: List<ReceivedPhoto>
  ): UpdateStateResult<List<ReceivedPhoto>> {
    val updatedPhotos = receivedPhotos.toMutableList()

    for (receivedPhoto in receivedPhotos) {
      val photoIndex = receivedPhotos
        .indexOfFirst { it.receivedPhotoName == receivedPhoto.receivedPhotoName }

      if (photoIndex != -1) {
        updatedPhotos[photoIndex] = receivedPhoto
      } else {
        updatedPhotos.add(receivedPhoto)
      }
    }

    val updatedSortedPhotos = updatedPhotos
      .sortedByDescending { it.uploadedOn }

    return UpdateStateResult.Update(updatedSortedPhotos)
  }
}