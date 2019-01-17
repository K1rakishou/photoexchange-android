package com.kirakishou.photoexchange.mvrx.viewmodel.state

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.mvrx.model.photo.GalleryPhoto

data class GalleryFragmentState(
  val favouritedPhotos: Set<String> = hashSetOf(),
  val reportedPhotos: Set<String> = hashSetOf(),

  val isEndReached: Boolean = false,
  val galleryPhotos: List<GalleryPhoto> = emptyList(),
  val galleryPhotosRequest: Async<Paged<GalleryPhoto>> = Uninitialized,

  val checkForFreshPhotosRequest: Async<Unit> = Uninitialized
) : MvRxState {

  fun onPhotoFavourited(
    photoName: String,
    isFavourited: Boolean,
    favouritesCount: Long
  ): UpdateStateResult<List<GalleryPhoto>> {
    val photoIndex = galleryPhotos.indexOfFirst { it.photoName == photoName }
    if (photoIndex == -1) {
      return UpdateStateResult.NothingToUpdate()
    }

    val updatedPhotos = galleryPhotos.toMutableList()
    val galleryPhoto = updatedPhotos[photoIndex]

    val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
      .copy(
        isFavourited = isFavourited,
        favouritesCount = favouritesCount
      )

    updatedPhotos[photoIndex] = galleryPhoto.copy(photoAdditionalInfo = updatedPhotoInfo)

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun onPhotoReported(
    photoName: String,
    isReported: Boolean
  ): UpdateStateResult<List<GalleryPhoto>> {
    val photoIndex = galleryPhotos.indexOfFirst { it.photoName == photoName }
    if (photoIndex == -1) {
      return UpdateStateResult.NothingToUpdate()
    }

    val updatedPhotos = galleryPhotos.toMutableList()
    val galleryPhoto = updatedPhotos[photoIndex]

    val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
      .copy(isReported = isReported)

    updatedPhotos[photoIndex] = galleryPhoto.copy(photoAdditionalInfo = updatedPhotoInfo)

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun removePhoto(photoName: String): UpdateStateResult<List<GalleryPhoto>> {
    val photoIndex = galleryPhotos.indexOfFirst { it.photoName == photoName }
    if (photoIndex == -1) {
      //nothing to remove
      return UpdateStateResult.NothingToUpdate()
    }

    val updatedPhotos = galleryPhotos.toMutableList().apply {
      removeAt(photoIndex)
    }

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun reportPhoto(
    photoName: String,
    reportResult: Boolean
  ): UpdateStateResult<List<GalleryPhoto>> {
    val photoIndex = galleryPhotos.indexOfFirst { it.photoName == photoName }
    if (photoIndex == -1) {
      return UpdateStateResult.NothingToUpdate()
    }

    val updatedPhotos = galleryPhotos.toMutableList()
    val galleryPhoto = updatedPhotos[photoIndex]

    val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
      .copy(isReported = reportResult)

    updatedPhotos[photoIndex] = galleryPhoto.copy(photoAdditionalInfo = updatedPhotoInfo)

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun favouritePhoto(
    photoName: String,
    isFavourited: Boolean,
    favouritesCount: Long
  ): UpdateStateResult<List<GalleryPhoto>> {
    val photoIndex = galleryPhotos.indexOfFirst { it.photoName == photoName }
    if (photoIndex == -1) {
      return UpdateStateResult.NothingToUpdate()
    }

    val updatedPhotos = galleryPhotos.toMutableList()
    val galleryPhoto = updatedPhotos[photoIndex]

    val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
      .copy(isFavourited = isFavourited, favouritesCount = favouritesCount)

    updatedPhotos[photoIndex] = galleryPhoto.copy(
      photoAdditionalInfo = updatedPhotoInfo
    )

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun swapPhotoAndMap(
    photoName: String
  ): UpdateStateResult<List<GalleryPhoto>> {
    val photoIndex = galleryPhotos.indexOfFirst { it.photoName == photoName }
    if (photoIndex == -1) {
      return UpdateStateResult.NothingToUpdate()
    }

    if (galleryPhotos[photoIndex].lonLat.isEmpty()) {
      return UpdateStateResult.SendIntercom()
    }

    val oldShowPhoto = galleryPhotos[photoIndex].showPhoto
    val updatedPhoto = galleryPhotos[photoIndex]
      .copy(showPhoto = !oldShowPhoto)

    val updatedPhotos = galleryPhotos.toMutableList()
    updatedPhotos[photoIndex] = updatedPhoto

    return UpdateStateResult.Update(updatedPhotos)
  }
}