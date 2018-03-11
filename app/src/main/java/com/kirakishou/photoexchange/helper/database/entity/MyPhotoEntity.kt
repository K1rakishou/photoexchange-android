package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import com.kirakishou.photoexchange.helper.database.converter.PhotoStateConverter
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity.Companion.TABLE_NAME
import com.kirakishou.photoexchange.mvp.model.PhotoState

/**
 * Created by kirakishou on 3/3/2018.
 */

@Entity(tableName = TABLE_NAME)
class MyPhotoEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID_COLUMN)
    var id: Long? = null,

    @ColumnInfo(name = PHOTO_STATE_COLUMN)
    @field:TypeConverters(PhotoStateConverter::class)
    var photoState: PhotoState

) {

    constructor() : this(null, PhotoState.PHOTO_TAKEN)

    fun isEmpty(): Boolean {
        return id == null
    }

    companion object {

        fun empty(): MyPhotoEntity {
            return MyPhotoEntity()
        }

        fun create(): MyPhotoEntity {
            return create(PhotoState.PHOTO_TAKEN)
        }

        fun create(photoState: PhotoState): MyPhotoEntity {
            return MyPhotoEntity(null, photoState)
        }

        const val ID_COLUMN = "ID"
        const val PHOTO_STATE_COLUMN = "PHOTO_STATE"

        const val TABLE_NAME = "MY_PHOTO"
    }
}