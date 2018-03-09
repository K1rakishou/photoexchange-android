package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.mvp.model.MyPhoto

/**
 * Created by kirakishou on 3/9/2018.
 */
object MyPhotoMapper {

    fun toMyPhoto(myPhotoEntityId: Long, myPhotoEntity: MyPhotoEntity?, tempFileEntity: TempFileEntity?): MyPhoto? {
        if (myPhotoEntity == null) {
            return null
        }

        val file = tempFileEntity?.asFile()
        return MyPhoto(myPhotoEntityId, myPhotoEntity.photoState!!, file)
    }
}