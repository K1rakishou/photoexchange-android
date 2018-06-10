package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity.Companion.TABLE_NAME
import java.io.File

/**
 * Created by kirakishou on 3/3/2018.
 */

@Entity(tableName = TABLE_NAME)
class TempFileEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID_COLUMN)
    var id: Long? = null,

    @ColumnInfo(name = TAKEN_PHOTO_ID_COLUMN, index = true)
    var takenPhotoId: Long? = null,

    @ColumnInfo(name = FILE_PATH_COLUMN)
    var filePath: String,

    @ColumnInfo(name = DELETED_ON_COLUMN, index = true)
    var deletedOn: Long = 0L

) {

    constructor() : this(null, 0L, "")

    fun isEmpty(): Boolean = this.filePath.isEmpty()

    fun asFile(): File {
        return File(filePath)
    }

    fun fileExists(): Boolean {
        return asFile().exists()
    }

    companion object {

        fun empty(): TempFileEntity {
            return TempFileEntity()
        }

        fun createEmpty(tempFilePath: String): TempFileEntity {
            return TempFileEntity(null, -1L, tempFilePath)
        }

        fun create(takenPhotoId: Long, tempFilePath: String): TempFileEntity {
            return TempFileEntity(null, takenPhotoId, tempFilePath)
        }

        const val TABLE_NAME = "TEMP_FILE"

        const val ID_COLUMN = "ID"
        const val TAKEN_PHOTO_ID_COLUMN = "TAKEN_PHOTO_ID"
        const val FILE_PATH_COLUMN = "FILE_PATH"
        const val DELETED_ON_COLUMN = "DELETED_ON"
    }
}