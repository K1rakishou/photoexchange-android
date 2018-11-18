package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.extension.hours
import com.kirakishou.photoexchange.helper.extension.mb
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.FileUtilsImpl
import com.kirakishou.photoexchange.helper.util.TimeUtils
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

open class TempFileRepository(
  private val filesDir: String,
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val fileUtils: FileUtils,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "TempFileRepository"
  private val tempFilesDao = database.tempFileDao()
  private val MAX_CACHE_SIZE = 50.mb()
  private val OLD_PHOTO_TIME_THRESHOLD = 1.hours()
  private val FILES_TO_DELETE_AT_A_TIME = 7

  suspend fun init() {
    withContext(coroutineContext) {
      createTempFilesDirIfNotExists()
    }
  }

  private suspend fun createTempFilesDirIfNotExists() {
    val fullPathFile = File(filesDir)
    if (!fullPathFile.exists()) {
      if (!fullPathFile.mkdirs()) {
        Timber.tag(TAG).w("Could not create directory ${fullPathFile.absolutePath}")
      }
    }
  }

  suspend fun create(): TempFileEntity {
    return withContext(coroutineContext) {
      val fullPathFile = File(filesDir)
      val file = File.createTempFile("file_", ".tmp", fullPathFile)

      return@withContext createInternal(file)
    }
  }

  private suspend fun createInternal(file: File): TempFileEntity {
    val entity = TempFileEntity.createEmpty(file.absolutePath)
    val insertedId = tempFilesDao.insert(entity)

    if (insertedId.isFail()) {
      return TempFileEntity.empty()
    }

    return entity.apply { this.id = insertedId }
  }

  suspend fun findById(id: Long): TempFileEntity {
    return withContext(coroutineContext) {
      return@withContext tempFilesDao.findById(id) ?: TempFileEntity.empty()
    }
  }

  suspend fun findAll(): List<TempFileEntity> {
    return withContext(coroutineContext) {
      return@withContext tempFilesDao.findAll()
    }
  }

  suspend fun findByFilePath(filePath: String): TempFileEntity {
    return withContext(coroutineContext) {
      return@withContext tempFilesDao.findByFilePath(filePath) ?: TempFileEntity.empty()
    }
  }

  suspend fun findDeletedOld(time: Long): List<TempFileEntity> {
    return withContext(coroutineContext) {
      return@withContext tempFilesDao.findDeletedOld(time)
    }
  }

  suspend fun findOldest(count: Int): List<TempFileEntity> {
    return withContext(coroutineContext) {
      return@withContext tempFilesDao.findOldest(count)
    }
  }

  suspend fun markDeletedById(tempFile: TempFileEntity): Int {
    return withContext(coroutineContext) {
      return@withContext markDeletedById(tempFile.id!!)
    }
  }

  suspend fun markDeletedById(id: Long): Int {
    return withContext(coroutineContext) {
      val time = timeUtils.getTimeFast()
      return@withContext tempFilesDao.markDeletedById(id, time)
    }
  }

  open suspend fun updateTakenPhotoId(tempFileEntity: TempFileEntity, takenPhotoId: Long): Int {
    return withContext(coroutineContext) {
      return@withContext tempFilesDao.updateTakenPhotoId(tempFileEntity.id!!, takenPhotoId)
    }
  }

  suspend fun deleteOld() {
    return withContext(coroutineContext) {
      deleteOld(OLD_PHOTO_TIME_THRESHOLD)
    }
  }

  suspend fun deleteOld(oldPhotoThreshold: Long) {
    return withContext(coroutineContext) {
      val oldFiles = tempFilesDao.findDeletedOld(oldPhotoThreshold)
      deleteMany(oldFiles)
    }
  }

  suspend fun deleteOldIfCacheSizeIsTooBig() {
    return withContext(coroutineContext) {
      val totalTempFilesCacheSize = fileUtils.calculateTotalDirectorySize(File(filesDir))
      if (totalTempFilesCacheSize > MAX_CACHE_SIZE) {
        val filesToDelete = tempFilesDao.findOldest(FILES_TO_DELETE_AT_A_TIME)
        if (filesToDelete.isEmpty()) {
          return@withContext
        }

        deleteMany(filesToDelete)
      }
    }
  }

  suspend fun deleteMany(tempFiles: List<TempFileEntity>) {
    return withContext(coroutineContext) {
      val filesOnDisk = mutableListOf<File>()

      tempFiles.forEach { oldFile ->
        if (tempFilesDao.deleteForReal(oldFile.id!!).isSuccess()) {
          filesOnDisk += oldFile.asFile()
        }
      }

      filesOnDisk.forEach { fileOnDisk ->
        deleteFileFromDisk(fileOnDisk)
      }
    }
  }

  suspend fun deleteEmptyTempFiles() {
    return withContext(coroutineContext) {
      val allEmptyFiles = tempFilesDao.findAllEmpty()

      allEmptyFiles.forEach {
        // do not delete a file record from the database if we could
        // not delete the file from the disk first
        if (deleteFileFromDisk(it.asFile())) {
          tempFilesDao.deleteForReal(it.id!!)
        }
      }
    }
  }

  private suspend fun deleteFileFromDisk(fileOnDisk: File): Boolean {
    return withContext(coroutineContext) {
      if (fileOnDisk.exists()) {
        if (!fileOnDisk.delete()) {
          Timber.tag(TAG).w("Could not delete file ${fileOnDisk.absoluteFile}")
          return@withContext false
        }
      }

      return@withContext true
    }
  }
}