package com.kirakishou.photoexchange.helper.repository.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.repository.entity.SentPhotoEntity.Companion.TABLE_NAME

/**
 * Created by kirakishou on 11/8/2017.
 */

@Entity(tableName = TABLE_NAME)
data class SentPhotoEntity(
        @PrimaryKey
        @ColumnInfo(name = "id")
        var id: Long,

        @ColumnInfo(name = "user_id", index = true)
        var userId: String,

        @ColumnInfo(name = "photo_name")
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
        const val TABLE_NAME = "sent_photo"
    }
}