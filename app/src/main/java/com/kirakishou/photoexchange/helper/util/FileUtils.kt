package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import timber.log.Timber
import java.io.File

/**
 * Created by kirakishou on 11/21/2017.
 */
object FileUtils {

    fun deletePhotoFile(takenPhototo: TakenPhoto) {
        val photoFile = File(takenPhototo.photoFilePath)
        if (photoFile.exists()) {
            val wasDeleted = photoFile.delete()
            if (!wasDeleted) {
                Timber.d("Could not delete file: ${takenPhototo.photoFilePath}")
            }
        }
    }

    fun deletePhotosFiles(allPhotos: List<TakenPhoto>) {
        allPhotos.forEach { takenPhoto ->
            deletePhotoFile(takenPhoto)
        }
    }
}