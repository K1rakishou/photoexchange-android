package com.kirakishou.photoexchange.mvp.model

import android.os.Bundle
import com.kirakishou.photoexchange.helper.database.converter.PhotoStateConverter
import java.io.File

/**
 * Created by kirakishou on 3/9/2018.
 */
data class MyPhoto(
    val id: Long,
    var photoState: PhotoState,
    val isPublic: Boolean = false,
    var photoName: String? = null,
    var photoTempFile: File? = null
) {

    fun isEmpty(): Boolean = this.id == 0L

    fun getFile(): File {
        return photoTempFile!!
    }

    fun toBundle(): Bundle {
        val outBundle = Bundle()
        outBundle.putLong("id", id)
        outBundle.putInt("photo_state", PhotoStateConverter.fromPhotoState(photoState))
        outBundle.putString("photo_temp_file", photoTempFile?.absolutePath ?: "")

        return outBundle
    }

    companion object {

        fun empty(): MyPhoto {
            return MyPhoto(0L, PhotoState.PHOTO_TAKEN, false, null)
        }

        fun fromBundle(bundle: Bundle?): MyPhoto {
            if (bundle == null) {
                return empty()
            }

            val id = bundle.getLong("id", -1L)
            if (id == -1L) {
                return empty()
            }

            val photoState = PhotoStateConverter.toPhotoState(bundle.getInt("photo_state"))
            val photoFileString = bundle.getString("photo_temp_file")
            val photoFile = if (photoFileString.isEmpty()) null else File(photoFileString)

            return MyPhoto(id, photoState, false, null, photoFile)
        }
    }
}