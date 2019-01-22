package com.kirakishou.photoexchange.mvrx.model.photo

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.mvrx.model.PhotoState
import java.io.File

class UploadingPhoto(
  id: Long,
  location: LonLat,
  isPublic: Boolean,
  photoName: String?,
  photoTempFile: File?,
  var progress: Int,
  photoState: PhotoState
) : TakenPhoto(id, location, isPublic, photoName, photoTempFile, photoState) {

  override fun hashCode(): Int {
    return id.hashCode() * progress.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other === this) {
      return true
    }

    if (other::class != this::class) {
      return false
    }

    other as UploadingPhoto

    return this.id == other.id && this.progress == other.progress
  }

  companion object {
    fun fromTakenPhoto(takenPhoto: TakenPhoto, progress: Int): UploadingPhoto {
      return UploadingPhoto(
        takenPhoto.id,
        takenPhoto.location,
        takenPhoto.isPublic,
        takenPhoto.photoName,
        takenPhoto.photoTempFile,
        progress,
        PhotoState.PHOTO_UPLOADING
      )
    }
  }
}