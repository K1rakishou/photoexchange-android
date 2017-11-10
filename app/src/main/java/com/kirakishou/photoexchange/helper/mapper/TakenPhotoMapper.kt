package com.kirakishou.photoexchange.helper.mapper

import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.model.TakenPhoto

/**
 * Created by kirakishou on 11/10/2017.
 */
class TakenPhotoMapper : Mapper {

    fun toTakenPhoto(entity: TakenPhotoEntity): TakenPhoto {
        return TakenPhoto(
                LonLat(entity.lon, entity.lat),
                entity.photoFilePath,
                entity.userId
        )
    }

    fun toTakenPhotos(entityList: List<TakenPhotoEntity>): List<TakenPhoto> {
        return entityList.map { toTakenPhoto(it) }
    }
}