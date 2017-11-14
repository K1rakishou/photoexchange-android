package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by kirakishou on 11/10/2017.
 */

@Entity(tableName = TakenPhotoEntity.TABLE_NAME)
class TakenPhotoEntity(

        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "id")
        var id: Long,

        @ColumnInfo(name = "lon")
        var lon: Double,

        @ColumnInfo(name = "lat")
        var lat: Double,

        @ColumnInfo(name = "photo_file_path")
        var photoFilePath: String,

        @ColumnInfo(name = "user_id")
        var userId: String
) {

    constructor() : this(0L, 0.0, 0.0, "", "")

    companion object {
        fun new(lon: Double, lat: Double, photoFilePath: String, userId: String) =
                TakenPhotoEntity(0L, lon, lat, photoFilePath, userId)

        const val TABLE_NAME = "taken_photos"
    }
}