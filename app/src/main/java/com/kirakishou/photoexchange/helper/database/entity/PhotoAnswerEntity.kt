package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer

/**
 * Created by kirakishou on 11/14/2017.
 */

@Entity(tableName = PhotoAnswerEntity.TABLE_NAME)
class PhotoAnswerEntity(

        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "id")
        var id: Long,

        @ColumnInfo(name = "photo_id")
        var photoRemoteId: Long,

        @ColumnInfo(name = "user_id")
        var userId: String,

        @ColumnInfo(name = "photo_name", index = true)
        var photoName: String,

        @ColumnInfo(name = "lon")
        var lon: Double,

        @ColumnInfo(name = "lat")
        var lat: Double
) {

    constructor() : this(0L, 0L, "", "", 0.0, 0.0)

    companion object {
        fun new(photoRemoteId: Long, userId: String, photoName: String, lon: Double, lat: Double) =
                PhotoAnswerEntity(0L, photoRemoteId, userId, photoName, lon, lat)

        fun fromPhotoAnswer(photoAnswer: PhotoAnswer): PhotoAnswerEntity {
            return PhotoAnswerEntity(
                    0L,
                    photoAnswer.photoRemoteId,
                    photoAnswer.userId,
                    photoAnswer.photoName,
                    photoAnswer.lon,
                    photoAnswer.lat
            )
        }

        const val TABLE_NAME = "photo_answer"
    }
}