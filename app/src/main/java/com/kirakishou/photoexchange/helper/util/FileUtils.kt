package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import timber.log.Timber
import java.io.File

/**
 * Created by kirakishou on 11/21/2017.
 */
object FileUtils {

    fun deletePhotoFiles(allPhotos: List<TakenPhoto>) {
        allPhotos.forEach { uploadedPhoto ->
            val photoFile = File(uploadedPhoto.photoFilePath)
            if (photoFile.exists()) {
                val wasDeleted = photoFile.delete()
                if (!wasDeleted) {
                    Timber.d("Could not delete file: ${uploadedPhoto.photoFilePath}")
                }
            }
        }
    }
}