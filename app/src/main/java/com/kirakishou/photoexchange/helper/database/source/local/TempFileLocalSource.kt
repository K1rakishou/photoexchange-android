package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.extension.hours
import com.kirakishou.photoexchange.helper.extension.mb
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import timber.log.Timber
import java.io.File

open class TempFileLocalSource(
  private val database: MyDatabase,
  private val filesDir: String,
  private val timeUtils: TimeUtils,
  private val fileUtils: FileUtils,
  private val maxCacheSize: Long,
  private val oldPhotoTimeThreshold: Long,
  private val fileToDeleteAtATime: Int
) {
  private val TAG = "TempFileLocalSource"
  private val tempFilesDao = database.tempFileDao()

  fun init() {
    createTempFilesDirIfNotExists()
  }

  private fun createTempFilesDirIfNotExists() {
    val fullPathFile = File(filesDir)
    if (!fullPathFile.exists()) {
      if (!fullPathFile.mkdirs()) {
        Timber.tag(TAG).w("Could not create directory ${fullPathFile.absolutePath}")
      }
    }
  }

  fun create(): TempFileEntity {
    val fullPathFile = File(filesDir)
    val file = File.createTempFile("file_", ".tmp", fullPathFile)

    val entity = TempFileEntity.createEmpty(file.absolutePath)
    val insertedId = tempFilesDao.insert(entity)

    if (insertedId.isFail()) {
      return TempFileEntity.empty()
    }

    return entity.apply { this.id = insertedId }
  }


  fun findById(id: Long): TempFileEntity {
    return tempFilesDao.findById(id) ?: TempFileEntity.empty()
  }

  fun findAll(): List<TempFileEntity> {
    return tempFilesDao.findAll()
  }

  fun findByFilePath(filePath: String): TempFileEntity {
    return tempFilesDao.findByFilePath(filePath) ?: TempFileEntity.empty()
  }

  fun findDeletedOld(time: Long): List<TempFileEntity> {
    return tempFilesDao.findDeletedOld(time)
  }

  fun findOldest(count: Int): List<TempFileEntity> {
    return tempFilesDao.findOldest(count)
  }

  fun markDeletedById(tempFile: TempFileEntity): Int {
    return markDeletedById(tempFile.id!!)
  }

  fun markDeletedById(id: Long): Int {
    val time = timeUtils.getTimeFast()
    return tempFilesDao.markDeletedById(id, time)
  }

  open fun updateTakenPhotoId(tempFileEntity: TempFileEntity, takenPhotoId: Long): Int {
    return tempFilesDao.updateTakenPhotoId(tempFileEntity.id!!, takenPhotoId)
  }

  fun deleteOld() {
    deleteOld(timeUtils.getTimeFast() - oldPhotoTimeThreshold)
  }

  fun deleteOld(oldPhotoThreshold: Long) {
    val oldFiles = tempFilesDao.findDeletedOld(oldPhotoThreshold)
    deleteMany(oldFiles)
  }

  fun deleteOldIfCacheSizeIsTooBig() {
    val totalTempFilesCacheSize = fileUtils.calculateTotalDirectorySize(File(filesDir))
    if (totalTempFilesCacheSize > maxCacheSize) {
      val filesToDelete = tempFilesDao.findOldest(fileToDeleteAtATime)
      if (filesToDelete.isEmpty()) {
        return
      }

      deleteMany(filesToDelete)
    }
  }

  fun deleteMany(tempFiles: List<TempFileEntity>) {
    val filesOnDisk = mutableListOf<File>()

    tempFiles.forEach { oldFile ->
      if (tempFilesDao.deleteForReal(oldFile.id!!).isSuccess()) {
        filesOnDisk += oldFile.asFile()
      } else {
        Timber.tag(TAG).w("Could not delete ${oldFile.filePath} from tempFiles table")
      }
    }

    filesOnDisk.forEach { fileOnDisk ->
      deleteFileFromDisk(fileOnDisk)
    }
  }

  fun deleteEmptyTempFiles() {
    val allEmptyFiles = tempFilesDao.findAllEmpty()

    allEmptyFiles.forEach {
      // do not delete a file record from the database if we could
      // not delete the file from the disk first
      if (deleteFileFromDisk(it.asFile())) {
        tempFilesDao.deleteForReal(it.id!!)
      }
    }
  }

  private fun deleteFileFromDisk(fileOnDisk: File): Boolean {
    if (fileOnDisk.exists()) {
      if (!fileOnDisk.delete()) {
        Timber.tag(TAG).w("Could not delete file ${fileOnDisk.absoluteFile}")
        return false
      }
    }

    return true
  }
}