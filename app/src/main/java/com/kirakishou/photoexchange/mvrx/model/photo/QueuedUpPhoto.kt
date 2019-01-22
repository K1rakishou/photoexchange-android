package com.kirakishou.photoexchange.mvrx.model.photo

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.mvrx.model.PhotoState
import java.io.File

class QueuedUpPhoto(
  id: Long,
  location: LonLat,
  isPublic: Boolean,
  photoName: String?,
  photoTempFile: File?,
  photoState: PhotoState
) : TakenPhoto(id, location, isPublic, photoName, photoTempFile, photoState) {

  companion object {
    fun fromTakenPhoto(takenPhoto: TakenPhoto): QueuedUpPhoto {
      return QueuedUpPhoto(
        takenPhoto.id,
        takenPhoto.location,
        takenPhoto.isPublic,
        takenPhoto.photoName,
        takenPhoto.photoTempFile,
        PhotoState.PHOTO_QUEUED_UP
      )
    }
  }
}