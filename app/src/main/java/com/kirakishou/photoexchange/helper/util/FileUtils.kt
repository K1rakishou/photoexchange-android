package com.kirakishou.photoexchange.helper.util

import java.io.File

interface FileUtils {
    fun createTempFile(prefix: String, suffix: String): File
    fun deleteFile(file: File)
    fun deleteFile(filePath: String)
    fun deleteAllFiles(directory: File)
    fun calculateTotalDirectorySize(directory: File): Long
}