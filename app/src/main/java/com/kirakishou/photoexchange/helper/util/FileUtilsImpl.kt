package com.kirakishou.photoexchange.helper.util

import timber.log.Timber
import java.io.File

/**
 * Created by kirakishou on 11/21/2017.
 */
open class FileUtilsImpl : FileUtils {

  val TAG = "FileUtilsImpl"

  override fun createTempFile(prefix: String, suffix: String): File {
    return File.createTempFile(prefix, suffix)
  }

  override fun deleteFile(file: File) {
    if (file.exists()) {
      val wasDeleted = file.delete()

      if (!wasDeleted) {
        Timber.tag(TAG).w("Could not delete file: ${file.absolutePath}")
      }
    }
  }

  override fun deleteFile(filePath: String) {
    deleteFile(File(filePath))
  }

  override fun deleteAllFiles(directory: File) {
    val files = directory.listFiles()
    if (files.isNotEmpty()) {
      files.forEach {
        if (it.isDirectory) {
          deleteAllFiles(it)
        }

        if (it.exists()) {
          deleteFile(it)
        }
      }
    }
  }

  override fun calculateTotalDirectorySize(directory: File): Long {
    var totalSize = 0L
    val files = directory.listFiles()

    if (files.isNotEmpty()) {
      files.forEach {
        if (it.isDirectory) {
          totalSize += calculateTotalDirectorySize(it)
        }

        totalSize += it.length()
      }
    }

    return totalSize
  }
}