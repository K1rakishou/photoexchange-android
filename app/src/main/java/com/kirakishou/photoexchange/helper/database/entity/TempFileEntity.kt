package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity.Companion.TABLE_NAME

/**
 * Created by kirakishou on 3/3/2018.
 */

@Entity(tableName = TABLE_NAME)
class TempFileEntity(

    @PrimaryKey
    @ColumnInfo(name = PHOTO_OWNER_ID_COLUMN)
    val photoOwnerId: Long,

    @ColumnInfo(name = FILE_PATH_COLUMN, index = true)
    val filePath: String
) {

    companion object {

        fun create(tempFilePath: String): TempFileEntity {
            return TempFileEntity(-1L, tempFilePath)
        }

        const val TABLE_NAME = "TEMP_FILE"

        const val PHOTO_OWNER_ID_COLUMN = "PHOTO_OWNER_ID"
        const val FILE_PATH_COLUMN = "FILE_PATH"
    }
}