package com.kirakishou.photoexchange.helper.mapper

import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.mvvm.model.TakenPhoto

/**
 * Created by kirakishou on 11/8/2017.
 */
class TakenPhotoMapper : Mapper {

    fun toTakenPhoto(entity: TakenPhotoEntity): TakenPhoto {
        return TakenPhoto(
                entity.id,
                entity.lon,
                entity.lat,
                entity.userId,
                entity.photoName,
                entity.photoFilePath,
                entity.wasSent
        )
    }

    fun toTakenPhoto(entityList: List<TakenPhotoEntity>): List<TakenPhoto> {
        return entityList.map { toTakenPhoto(it) }
    }
}