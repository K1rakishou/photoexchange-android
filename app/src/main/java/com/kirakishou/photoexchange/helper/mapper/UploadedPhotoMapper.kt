package com.kirakishou.photoexchange.helper.mapper

import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.mvvm.model.UploadedPhoto

/**
 * Created by kirakishou on 11/8/2017.
 */
class UploadedPhotoMapper : Mapper {

    fun toTakenPhoto(entity: UploadedPhotoEntity): UploadedPhoto {
        return UploadedPhoto(
                entity.id,
                entity.lon,
                entity.lat,
                entity.userId,
                entity.photoName,
                entity.photoFilePath
        )
    }

    fun toTakenPhoto(entityList: List<UploadedPhotoEntity>): List<UploadedPhoto> {
        return entityList.map { toTakenPhoto(it) }
    }
}