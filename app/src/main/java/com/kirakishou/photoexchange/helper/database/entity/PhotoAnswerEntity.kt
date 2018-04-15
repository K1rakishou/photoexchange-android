package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
class PhotoAnswerEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID_COLUMN)
    var id: Long? = null,

    @ColumnInfo(name = PHOTO_ANSWER_NAME_COLUMN)
    var photoAnswerName: String? = null,

    @ColumnInfo(name = LON_COLUMN)
    var lon: Double? = null,

    @ColumnInfo(name = LAT_COLUMN)
    var lat: Double? = null
) {

    fun isEmpty(): Boolean {
        return id == null
    }

    companion object {

        fun empty(): PhotoAnswerEntity {
            return PhotoAnswerEntity()
        }

        const val TABLE_NAME = "PHOTO_ANSWER"

        const val ID_COLUMN = "ID"
        const val PHOTO_ANSWER_NAME_COLUMN = "PHOTO_ANSWER_NAME"
        const val LON_COLUMN = "LON"
        const val LAT_COLUMN = "LAT"
    }
}