package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.mwvm.model.other.Constants
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import timber.log.Timber
import java.io.File

/**
 * Created by kirakishou on 11/21/2017.
 */
object FileUtils {

    fun deletePhotoFile(takenPhoto: TakenPhoto) {
        val photoFile = File(takenPhoto.photoFilePath)
        if (photoFile.exists()) {
            val wasDeleted = photoFile.delete()
            if (Constants.isDebugBuild) {
                check(wasDeleted, { "Could not delete file: ${takenPhoto.photoFilePath}" })
            }
        }
    }

    fun deletePhotosFiles(allPhotos: List<TakenPhoto>) {
        allPhotos.forEach { takenPhoto ->
            deletePhotoFile(takenPhoto)
        }
    }
}