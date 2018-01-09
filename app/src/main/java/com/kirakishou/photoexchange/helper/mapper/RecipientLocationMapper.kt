package com.kirakishou.photoexchange.helper.mapper

import com.kirakishou.photoexchange.helper.database.entity.RecipientLocationEntity
import com.kirakishou.photoexchange.mwvm.model.other.RecipientLocation

/**
 * Created by kirakishou on 1/8/2018.
 */
class RecipientLocationMapper : Mapper {

    fun toRecipientLocation(entity: RecipientLocationEntity) =
            RecipientLocation.fromEntity(entity)

    fun toRecipientLocations(entityList: List<RecipientLocationEntity>) =
            entityList.map { RecipientLocation.fromEntity(it) }
}