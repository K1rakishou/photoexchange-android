package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.mvrx.model.PhotoState
import com.kirakishou.photoexchange.mvrx.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvrx.model.photo.UploadingPhoto

/**
 * Created by kirakishou on 3/9/2018.
 */
object TakenPhotosMapper {

  fun toTakenPhoto(takenPhotoEntity: TakenPhotoEntity, tempFileEntity: TempFileEntity): TakenPhoto? {
    if (takenPhotoEntity.id == null || takenPhotoEntity.id!! <= 0L) {
      return null
    }

    val file = if (tempFileEntity.isEmpty()) {
      null
    } else {
      tempFileEntity.asFile()
    }

    return when (takenPhotoEntity.photoState) {
      PhotoState.PHOTO_TAKEN -> TakenPhoto(
        takenPhotoEntity.id!!,
        LonLat(takenPhotoEntity.lon, takenPhotoEntity.lat),
        takenPhotoEntity.isPublic,
        takenPhotoEntity.photoName,
        file,
        takenPhotoEntity.photoState
      )
      PhotoState.PHOTO_QUEUED_UP -> TakenPhoto(
        takenPhotoEntity.id!!,
        LonLat(takenPhotoEntity.lon, takenPhotoEntity.lat),
        takenPhotoEntity.isPublic,
        takenPhotoEntity.photoName,
        file,
        takenPhotoEntity.photoState
      )
      PhotoState.PHOTO_UPLOADING -> UploadingPhoto(
        takenPhotoEntity.id!!,
        LonLat(takenPhotoEntity.lon, takenPhotoEntity.lat),
        takenPhotoEntity.isPublic,
        takenPhotoEntity.photoName,
        file,
        0,
        takenPhotoEntity.photoState
      )
    }
  }
}