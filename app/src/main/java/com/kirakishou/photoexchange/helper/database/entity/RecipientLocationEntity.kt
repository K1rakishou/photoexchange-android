package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.mwvm.model.other.RecipientLocation

/**
 * Created by kirakishou on 1/8/2018.
 */

@Entity(tableName = RecipientLocationEntity.TABLE_NAME)
class RecipientLocationEntity(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "photo_name")
    var photoName: String,

    @ColumnInfo(name = "lon")
    var lon: Double,

    @ColumnInfo(name = "lat")
    var lat: Double
) {

    constructor() : this("", 0.0, 0.0)

    companion object {
        fun new(photoName: String, lon: Double, lat: Double): RecipientLocationEntity {
            return RecipientLocationEntity(photoName, lon, lat)
        }

        fun fromRecipientLocation(recipientLocation: RecipientLocation): RecipientLocationEntity {
            return RecipientLocationEntity(recipientLocation.photoName, recipientLocation.lon, recipientLocation.lat)
        }

        const val TABLE_NAME = "recipient_location"
    }
}