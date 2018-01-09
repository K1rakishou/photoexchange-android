package com.kirakishou.photoexchange.mwvm.model.other

import com.kirakishou.photoexchange.helper.database.entity.RecipientLocationEntity
import com.kirakishou.photoexchange.mwvm.model.net.response.UserNewLocationJsonObject

/**
 * Created by kirakishou on 1/8/2018.
 */
class RecipientLocation(
    var photoName: String,
    var lon: Double,
    var lat: Double
) {
    fun getLocation(): LonLat {
        return LonLat(lon, lat)
    }

    companion object {
        fun fromEntity(entity: RecipientLocationEntity): RecipientLocation {
            return RecipientLocation(entity.photoName, entity.lon, entity.lat)
        }

        fun fromUserNewLocationJsonObject(jsonObject: UserNewLocationJsonObject): RecipientLocation {
            return RecipientLocation(jsonObject.photoName, jsonObject.lon, jsonObject.lat)
        }
    }
}