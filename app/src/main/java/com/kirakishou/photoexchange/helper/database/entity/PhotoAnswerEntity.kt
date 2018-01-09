package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer

/**
 * Created by kirakishou on 11/14/2017.
 */

@Entity(tableName = PhotoAnswerEntity.TABLE_NAME)
data class PhotoAnswerEntity(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "photo_id")
    var photoRemoteId: Long,

    @ColumnInfo(name = "user_id")
    var userId: String,

    @ColumnInfo(name = "photo_name", index = true)
    var photoName: String,

    @ColumnInfo(name = "lon")
    var lon: Double,

    @ColumnInfo(name = "lat")
    var lat: Double,

    @ColumnInfo(name = "created_on", index = true)
    var createdOn: Long
) {

    constructor() : this(0L, "", "", 0.0, 0.0, 0L)

    companion object {
        fun new(photoRemoteId: Long, userId: String, photoName: String, lon: Double, lat: Double) =
                PhotoAnswerEntity(photoRemoteId, userId, photoName, lon, lat, TimeUtils.getTimeFast())

        const val TABLE_NAME = "photo_answer"
    }
}