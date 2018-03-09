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

@Entity(tableName = TABLE_NAME,
    foreignKeys = [(ForeignKey(
        entity = MyPhotoEntity::class,
        parentColumns = [MyPhotoEntity.ID_COLUMN],
        childColumns = [TempFileEntity.PHOTO_OWNER_ID_COLUMN],
        onDelete = ForeignKey.CASCADE))])
class TempFileEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID_COLUMN)
    var id: Long? = null,

    @ColumnInfo(name = PHOTO_OWNER_ID_COLUMN, index = true)
    var photoOwnerId: Long? = null,

    @ColumnInfo(name = FILE_PATH_COLUMN, index = true)
    val filePath: String
) {

    fun asFile(): File {
        return File(filePath)
    }

    companion object {

        fun create(photoOwnerId: Long, tempFilePath: String): TempFileEntity {
            return TempFileEntity(null, photoOwnerId, tempFilePath)
        }

        const val TABLE_NAME = "TEMP_FILE"

        const val ID_COLUMN = "ID"
        const val PHOTO_OWNER_ID_COLUMN = "PHOTO_OWNER_ID"
        const val FILE_PATH_COLUMN = "FILE_PATH"
    }
}