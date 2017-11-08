package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.util.TimeUtils

/**
 * Created by kirakishou on 11/8/2017.
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

        @ColumnInfo(name = "user_id")
        var userId: String,

        @ColumnInfo(name = "photo_name")
        var photoName: String,

        @ColumnInfo(name = "photo_file_path")
        var photoFilePath: String,

        @ColumnInfo(name = "failed_to_upload")
        var failedToUpload: Boolean,

        @ColumnInfo(name = "was_sent")
        var wasSent: Boolean,

        @ColumnInfo(name = "created_on")
        var createdOn: Long
) {

    constructor() : this(0L, 0.0, 0.0, "", "", "", false, false, 0L)

    companion object {

        fun new(lon: Double, lat: Double, userId: String, photoFilePath: String): TakenPhotoEntity {
            return TakenPhotoEntity(0L, lon, lat, userId, "", photoFilePath, false, false, TimeUtils.getTimeFast())
        }

        const val TABLE_NAME = "taken_photo"
    }
}