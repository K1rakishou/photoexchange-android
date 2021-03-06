package com.kirakishou.photoexchange.mvrx.viewmodel.state

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.mvrx.model.PhotoState
import com.kirakishou.photoexchange.mvrx.model.photo.*

data class UploadedPhotosFragmentState(
  val takenPhotos: List<TakenPhoto> = emptyList(),
  val takenPhotosRequest: Async<List<TakenPhoto>> = Uninitialized,

  val isEndReached: Boolean = false,
  val uploadedPhotos: List<UploadedPhoto> = emptyList(),
  val uploadedPhotosRequest: Async<Paged<UploadedPhoto>> = Uninitialized,

  val checkForFreshPhotosRequest: Async<Unit> = Uninitialized
) : MvRxState {

  fun swapPhotoAndMap(uploadedPhotoName: String): UpdateStateResult<List<UploadedPhoto>> {
    val photoIndex = uploadedPhotos.indexOfFirst { it.photoName == uploadedPhotoName }
    if (photoIndex == -1) {
      return UpdateStateResult.NothingToUpdate()
    }

    if (uploadedPhotos[photoIndex].receiverInfo == null) {
      return UpdateStateResult.SendIntercom()
    }

    val oldShowPhoto = uploadedPhotos[photoIndex].showPhoto
    val updatedPhoto = uploadedPhotos[photoIndex]
      .copy(showPhoto = !oldShowPhoto)

    val updatedPhotos = uploadedPhotos.toMutableList()
    updatedPhotos[photoIndex] = updatedPhoto

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun onPhotoUploadingProgress(photo: TakenPhoto, progress: Int): UpdateStateResult<List<TakenPhoto>> {
    val photoIndex = takenPhotos.indexOfFirst { takenPhoto ->
      takenPhoto.id == photo.id && takenPhoto.photoState == PhotoState.PHOTO_UPLOADING
    }

    val updatedPhotos = takenPhotos.toMutableList()

    if (photoIndex != -1) {
      updatedPhotos.removeAt(photoIndex)
      updatedPhotos.add(photoIndex, UploadingPhoto.fromTakenPhoto(photo, progress))
    } else {
      updatedPhotos.add(0, UploadingPhoto.fromTakenPhoto(photo, progress))
    }

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun onPhotoUploaded(
    photo: TakenPhoto,
    newPhotoId: Long,
    newPhotoName: String,
    uploadedOn: Long,
    currentLocation: LonLat
  ): UpdateStateResult<Pair<List<TakenPhoto>, List<UploadedPhoto>>> {
    val photoIndex = takenPhotos.indexOfFirst { takenPhoto ->
      takenPhoto.id == photo.id && takenPhoto.photoState == PhotoState.PHOTO_UPLOADING
    }

    val newTakenPhotos = takenPhotos.toMutableList()

    if (photoIndex != -1) {
      newTakenPhotos.removeAt(photoIndex)
    }

    val newUploadedPhotos = uploadedPhotos.toMutableList()
    val newUploadedPhoto = UploadedPhoto(
      newPhotoId,
      newPhotoName,
      currentLocation.lon,
      currentLocation.lat,
      null,
      uploadedOn
    )

    newUploadedPhotos.add(newUploadedPhoto)
    newUploadedPhotos.sortByDescending { it.photoId }

    return UpdateStateResult.Update(newTakenPhotos to newUploadedPhotos)
  }

  fun onFailedToUploadPhoto(photo: TakenPhoto): UpdateStateResult<List<TakenPhoto>> {
    val photoIndex = takenPhotos.indexOfFirst { takenPhoto ->
      takenPhoto.id == photo.id && takenPhoto.photoState == PhotoState.PHOTO_UPLOADING
    }

    val updatedPhotos = takenPhotos.toMutableList()

    if (photoIndex != -1) {
      updatedPhotos.removeAt(photoIndex)
      updatedPhotos.add(photoIndex, QueuedUpPhoto.fromTakenPhoto(photo))
    } else {
      updatedPhotos.add(0, QueuedUpPhoto.fromTakenPhoto(photo))
    }

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun onPhotosReceived(receivedPhotos: List<ReceivedPhoto>): UpdateStateResult<List<UploadedPhoto>> {
    val updatedPhotos = mutableListOf<UploadedPhoto>()

    for (uploadedPhoto in uploadedPhotos) {
      val exchangedPhoto = receivedPhotos.firstOrNull { receivedPhoto ->
        receivedPhoto.uploadedPhotoName == uploadedPhoto.photoName
      }

      updatedPhotos += if (exchangedPhoto == null) {
        uploadedPhoto.copy()
      } else {
        val receiverInfo = UploadedPhoto.ReceiverInfo(
          exchangedPhoto.receivedPhotoName,
          exchangedPhoto.lonLat
        )

        uploadedPhoto.copy(receiverInfo = receiverInfo)
      }
    }

    return UpdateStateResult.Update(updatedPhotos)
  }
}