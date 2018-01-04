package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.mwvm.model.other.Constants
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import java.io.File

/**
 * Created by kirakishou on 11/21/2017.
 */
object FileUtils {

    fun deleteFile(file: File) {
        if (file.exists()) {
            val wasDeleted = file.delete()
            if (Constants.isDebugBuild) {
                check(wasDeleted, { "Could not delete file: ${file.absolutePath}" })
            }
        }
    }

    fun deletePhotoFile(takenPhoto: TakenPhoto) {
        deleteFile(File(takenPhoto.photoFilePath))
    }

    fun deletePhotosFiles(allPhotos: List<TakenPhoto>) {
        allPhotos.forEach { takenPhoto ->
            deletePhotoFile(takenPhoto)
        }
    }
}