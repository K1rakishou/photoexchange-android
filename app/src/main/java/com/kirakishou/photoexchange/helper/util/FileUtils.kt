package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.mvp.model.other.Constants
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

    fun deleteFile(filePath: String) {
        deleteFile(File(filePath))
    }
}