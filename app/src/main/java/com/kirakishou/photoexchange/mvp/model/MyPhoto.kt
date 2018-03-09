package com.kirakishou.photoexchange.mvp.model

import com.kirakishou.photoexchange.mvp.model.state.PhotoState
import java.io.File

/**
 * Created by kirakishou on 3/9/2018.
 */
data class MyPhoto(
    val id: Long,
    val photoState: PhotoState,
    val photoTempFile: File? = null
) {
    fun getFile(): File {
        return photoTempFile!!
    }
}