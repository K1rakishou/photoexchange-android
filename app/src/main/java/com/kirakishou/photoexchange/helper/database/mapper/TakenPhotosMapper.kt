package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.mvp.model.TakenPhoto

/**
 * Created by kirakishou on 3/9/2018.
 */
object TakenPhotosMapper {

  fun toMyPhoto(takenPhotoEntity: TakenPhotoEntity, tempFileEntity: TempFileEntity): TakenPhoto {
    if (takenPhotoEntity.id == null || takenPhotoEntity.id!! <= 0L) {
      return TakenPhoto.empty()
    }

    val file = if (tempFileEntity.isEmpty()) {
      null
    } else {
      tempFileEntity.asFile()
    }

    return TakenPhoto(takenPhotoEntity.id!!, takenPhotoEntity.photoState, takenPhotoEntity.isPublic, takenPhotoEntity.photoName, file)
  }
}