package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity.Companion.TABLE_NAME

/**
 * Created by kirakishou on 3/3/2018.
 */

@Entity(tableName = TABLE_NAME)
class MyPhotoEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID_COLUMN)
    val id: Long = -1L,

    @ColumnInfo(name = PHOTO_FILE_ID_COLUMN)
    val photoFileId: Long
) {

    companion object {

        fun create(photoFileId: Long): MyPhotoEntity {
            return MyPhotoEntity(-1L, photoFileId)
        }

        const val ID_COLUMN = "ID"
        const val PHOTO_FILE_ID_COLUMN = "PHOTO_FILE_ID"

        const val TABLE_NAME = "MY_PHOTO"
    }
}