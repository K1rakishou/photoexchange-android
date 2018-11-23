package com.kirakishou.photoexchange.mvp.model.photo

import com.kirakishou.photoexchange.mvp.model.PhotoState
import java.io.File

class UploadingPhoto(
  id: Long,
  isPublic: Boolean,
  photoName: String?,
  photoTempFile: File?,
  var progress: Int,
  photoState: PhotoState
) : TakenPhoto(id, isPublic, photoName, photoTempFile, photoState) {

  companion object {
    fun fromMyPhoto(takenPhoto: TakenPhoto, progress: Int): UploadingPhoto {
      return UploadingPhoto(
        takenPhoto.id,
        takenPhoto.isPublic,
        takenPhoto.photoName,
        takenPhoto.photoTempFile,
        progress,
        PhotoState.PHOTO_UPLOADING
      )
    }
  }
}