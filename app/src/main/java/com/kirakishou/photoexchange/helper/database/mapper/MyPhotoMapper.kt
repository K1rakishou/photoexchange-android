package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.mvp.model.MyPhoto

/**
 * Created by kirakishou on 3/9/2018.
 */
object MyPhotoMapper {

    fun toMyPhoto(myPhotoEntity: MyPhotoEntity, tempFileEntity: TempFileEntity): MyPhoto {
        if (myPhotoEntity.id == null || myPhotoEntity.id!! <= 0L) {
            return MyPhoto.empty()
        }

        val file = if (tempFileEntity.isEmpty()) {
            null
        } else {
            tempFileEntity.asFile()
        }

        return MyPhoto(myPhotoEntity.id!!, myPhotoEntity.photoState, myPhotoEntity.photoName, file)
    }
}