package com.kirakishou.photoexchange.mvp.model.photo

import com.kirakishou.photoexchange.mvp.model.PhotoState
import java.io.File

class FailedToUploadPhoto(
  id: Long,
  isPublic: Boolean,
  photoName: String?,
  photoTempFile: File?,
  photoState: PhotoState
) : TakenPhoto(id, isPublic, photoName, photoTempFile, photoState) {

  companion object {
    fun fromTakenPhoto(takenPhoto: TakenPhoto): FailedToUploadPhoto {
      return FailedToUploadPhoto(
        takenPhoto.id,
        takenPhoto.isPublic,
        takenPhoto.photoName,
        takenPhoto.photoTempFile,
        PhotoState.FAILED_TO_UPLOAD
      )
    }
  }
}