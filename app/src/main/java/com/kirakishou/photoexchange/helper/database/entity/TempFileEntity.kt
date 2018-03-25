package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
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

    @ColumnInfo(name = FILE_PATH_COLUMN)
    var filePath: String

) {

    constructor() : this(null, "")

    fun isEmpty(): Boolean = this.filePath.isEmpty()

    fun asFile(): File {
        return File(filePath)
    }

    companion object {

        fun empty(): TempFileEntity {
            return TempFileEntity()
        }

        fun create(tempFilePath: String): TempFileEntity {
            return TempFileEntity(null, tempFilePath)
        }

        const val TABLE_NAME = "TEMP_FILE"

        const val ID_COLUMN = "ID"
        const val FILE_PATH_COLUMN = "FILE_PATH"
    }
}