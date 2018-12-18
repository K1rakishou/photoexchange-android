package com.kirakishou.photoexchange.mvp.viewmodel.state

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.photo.*

data class UploadedPhotosFragmentState(
  val takenPhotos: List<TakenPhoto> = emptyList(),

  val isEndReached: Boolean = false,
  val uploadedPhotos: List<UploadedPhoto> = emptyList(),
  val uploadedPhotosRequest: Async<Paged<UploadedPhoto>> = Uninitialized
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

  fun updateReceiverInfo(
    photoExchangedData: PhotoExchangedData
  ): UpdateStateResult<List<UploadedPhoto>> {
    val photoIndex = uploadedPhotos.indexOfFirst { uploadedPhoto ->
      uploadedPhoto.photoName == photoExchangedData.uploadedPhotoName
    }
    if (photoIndex == -1) {
      //nothing to update
      return UpdateStateResult.NothingToUpdate()
    }

    val updatedPhotos = uploadedPhotos.toMutableList()
    val receiverInfo = UploadedPhoto.ReceiverInfo(
      photoExchangedData.receivedPhotoName,
      LonLat(
        photoExchangedData.lon,
        photoExchangedData.lat
      )
    )

    val updatedPhoto = updatedPhotos[photoIndex]
      .copy(receiverInfo = receiverInfo)

    updatedPhotos.removeAt(photoIndex)
    updatedPhotos.add(photoIndex, updatedPhoto)

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun replaceQueuedUpPhotoWithUploading(photo: TakenPhoto): UpdateStateResult<List<TakenPhoto>> {
    val photoIndex = takenPhotos
      .indexOfFirst { it.id == photo.id && it.photoState == PhotoState.PHOTO_QUEUED_UP }

    val updatedPhotos = takenPhotos.toMutableList()

    if (photoIndex != -1) {
      updatedPhotos.removeAt(photoIndex)
      updatedPhotos.add(photoIndex, UploadingPhoto.fromMyPhoto(photo, 0))
    } else {
      updatedPhotos.add(0, UploadingPhoto.fromMyPhoto(photo, 0))
    }

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun updateUploadingPhotoProgress(photo: TakenPhoto, progress: Int): UpdateStateResult<List<TakenPhoto>> {
    val photoIndex = takenPhotos.indexOfFirst { takenPhoto ->
      takenPhoto.id == photo.id && takenPhoto.photoState == PhotoState.PHOTO_UPLOADING
    }

    val updatedPhotos = takenPhotos.toMutableList()

    if (photoIndex != -1) {
      updatedPhotos.removeAt(photoIndex)
      updatedPhotos.add(photoIndex, UploadingPhoto.fromMyPhoto(photo, progress))
    } else {
      updatedPhotos.add(0, UploadingPhoto.fromMyPhoto(photo, progress))
    }

    return UpdateStateResult.Update(updatedPhotos)
  }

  fun replaceUploadingPhotoWithUploaded(
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
    newUploadedPhotos.sortByDescending { it.uploadedOn }

    return UpdateStateResult.Update(newTakenPhotos to newUploadedPhotos)
  }

  fun replaceUploadingPhotoWithFailed(photo: TakenPhoto): UpdateStateResult<List<TakenPhoto>> {
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

  fun updateReceiverInfo(receivedPhotos: List<ReceivedPhoto>): UpdateStateResult<List<UploadedPhoto>> {
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