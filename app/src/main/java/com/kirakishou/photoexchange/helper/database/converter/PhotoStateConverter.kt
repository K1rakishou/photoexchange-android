package com.kirakishou.photoexchange.helper.database.converter

import android.arch.persistence.room.TypeConverter
import com.kirakishou.photoexchange.mvp.model.state.PhotoState

/**
 * Created by kirakishou on 3/8/2018.
 */
object PhotoStateConverter {

    @TypeConverter
    @JvmStatic
    fun toPhotoState(photoStateInt: Int): PhotoState {
        return PhotoState.from(photoStateInt)
    }

    @TypeConverter
    @JvmStatic
    fun fromPhotoState(photoState: PhotoState): Int {
        return photoState.state
    }
}