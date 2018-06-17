package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.*
import com.kirakishou.photoexchange.helper.database.entity.CachedPhotoIdEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
class CachedPhotoIdEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID_COLUMN)
    var id: Long? = null,

    @ColumnInfo(name = PHOTO_ID_COLUMN)
    var photoId: Long = -1L,

    @field:TypeConverters(CachedPhotoIdTypeConverter::class)
    @ColumnInfo(name = PHOTO_TYPE_COLUMN)
    var photoType: PhotoType = PhotoType.UploadedPhoto
) {

    fun isEmpty(): Boolean {
        return id == -1L
    }

    companion object {

        fun empty(): CachedPhotoIdEntity {
            return CachedPhotoIdEntity(-1L)
        }

        fun create(id: Long?, photoId: Long, photoType: PhotoType): CachedPhotoIdEntity {
            return CachedPhotoIdEntity(id, photoId, photoType)
        }

        const val TABLE_NAME = "CACHED_PHOTO_ID"

        const val ID_COLUMN = "ID"
        const val PHOTO_ID_COLUMN = "PHOTO_ID"
        const val PHOTO_TYPE_COLUMN = "PHOTO_TYPE"
    }

    enum class PhotoType(val value: Int) {
        UploadedPhoto(0),
        ReceivedPhoto(1),
        GalleryPhoto(2);

        companion object {
            fun fromValue(value: Int): PhotoType {
                return PhotoType.values().first { it.value == value }
            }
        }
    }

    object CachedPhotoIdTypeConverter {

        @TypeConverter
        @JvmStatic
        fun toValue(photoType: PhotoType): Int {
            return photoType.value
        }

        @TypeConverter
        @JvmStatic
        fun fromValue(value: Int): PhotoType {
            return PhotoType.fromValue(value)
        }
    }

}