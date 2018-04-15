package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import com.kirakishou.photoexchange.helper.database.converter.PhotoStateConverter
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity.Companion.TABLE_NAME
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState

/**
 * Created by kirakishou on 3/3/2018.
 */

@Entity(tableName = TABLE_NAME)
class MyPhotoEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID_COLUMN)
    var id: Long? = null,

    @ColumnInfo(name = TEMP_FILE_ID_COLUMN)
    var tempFileId: Long? = null,

    @ColumnInfo(name = IS_PUBLIC_COLUMN)
    var isPublic: Boolean = false,

    @ColumnInfo(name = PHOTO_NAME_COLUMN)
    var photoName: String? = null,

    @ColumnInfo(name = PHOTO_STATE_COLUMN)
    @field:TypeConverters(PhotoStateConverter::class)
    var photoState: PhotoState = PhotoState.PHOTO_TAKEN,

    @ColumnInfo(name = TAKEN_ON_COLUMN, index = true)
    var takenOn: Long? = null

) {

    fun isEmpty(): Boolean {
        return id == null
    }

    companion object {

        fun empty(): MyPhotoEntity {
            return MyPhotoEntity()
        }

        fun create(tempFileId: Long?, isPublic: Boolean): MyPhotoEntity {
            return MyPhotoEntity(null, tempFileId, isPublic, null, PhotoState.PHOTO_TAKEN, TimeUtils.getTimeFast())
        }

        const val TABLE_NAME = "MY_PHOTO"

        const val ID_COLUMN = "ID"
        const val PHOTO_STATE_COLUMN = "PHOTO_STATE"
        const val TEMP_FILE_ID_COLUMN = "TEMP_FILE_ID"
        const val IS_PUBLIC_COLUMN = "IS_PUBLIC"
        const val PHOTO_NAME_COLUMN = "PHOTO_NAME"
        const val TAKEN_ON_COLUMN = "TAKEN_ON"
    }
}