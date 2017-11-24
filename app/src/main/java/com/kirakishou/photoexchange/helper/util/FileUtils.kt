package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import timber.log.Timber
import java.io.File

/**
 * Created by kirakishou on 11/21/2017.
 */
object FileUtils {

    fun deletePhotoFile(takenPhotooto: TakenPhoto) {
        val photoFile = File(takenPhotooto.photoFilePath)
        if (photoFile.exists()) {
            val wasDeleted = photoFile.delete()
            if (!wasDeleted) {
                Timber.d("Could not delete file: ${takenPhotooto.photoFilePath}")
            }
        }
    }

    fun deletePhotosFiles(allPhotos: List<TakenPhoto>) {
        allPhotos.forEach { takenPhoto ->
            deletePhotoFile(takenPhoto)
        }
    }
}