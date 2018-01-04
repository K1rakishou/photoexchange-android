package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mwvm.model.state.PhotoState

/**
 * Created by kirakishou on 11/10/2017.
 */

@Entity(tableName = TakenPhotoEntity.TABLE_NAME)
data class TakenPhotoEntity(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "id")
        var id: Long,

        @ColumnInfo(name = "lon")
        var lon: Double,

        @ColumnInfo(name = "lat")
        var lat: Double,

        @ColumnInfo(name = "user_id")
        var userId: String,

        @ColumnInfo(name = "photo_name")
        var photoName: String,

        @ColumnInfo(name = "photo_file_path")
        var photoFilePath: String,

        @ColumnInfo(name = "photo_state")
        var photoState: String,

        @ColumnInfo(name = "created_on", index = true)
        var createdOn: Long
) {

    constructor() : this(0L, 0.0, 0.0, "", "", "", PhotoState.QUEUED_UP.value, 0L)

    fun isEmpty(): Boolean {
        return id == -1L
    }

    companion object {
        fun new(lon: Double, lat: Double, userId: String, photoFilePath: String, state: PhotoState) =
                TakenPhotoEntity(0L, lon, lat, userId, "", photoFilePath, state.value, TimeUtils.getTimeFast())

        fun empty() =
                TakenPhotoEntity(-1L, 0.0, 0.0, "", "", "", PhotoState.TAKEN_PHOTO_STATE, 0L)

        const val TABLE_NAME = "taken_photos"
    }
}