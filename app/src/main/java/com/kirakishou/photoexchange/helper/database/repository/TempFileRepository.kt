package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.util.TimeUtils
import timber.log.Timber
import java.io.File

open class TempFileRepository(
    private val filesDir: String,
    private val database: MyDatabase,
    private val timeUtils: TimeUtils
) {
    val TAG = "TempFileRepository"
    val tempFilesDao = database.tempFileDao()

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

        val entity = TempFileEntity.create(file.absolutePath)
        val insertedId = tempFilesDao.insert(entity)

        if (insertedId.isFail()) {
            return TempFileEntity.empty()
        }

        return entity.apply { this.id = insertedId }
    }

    fun findById(id: Long): TempFileEntity? {
        return tempFilesDao.findById(id)
    }

    fun findAll(): List<TempFileEntity> {
        return tempFilesDao.findAll()
    }

    fun findByFilePath(filePath: String): TempFileEntity? {
        return tempFilesDao.findByFilePath(filePath)
    }

    fun markDeletedByFilePath(filePath: String) {
        tempFilesDao.markDeletedByFilePath(filePath, timeUtils.getTimeFast())
    }

    fun findDeletedOld(time: Long): List<TempFileEntity> {
        return tempFilesDao.findDeletedOld(time)
    }

    fun markDeletedById(id: Long) {
        tempFilesDao.markDeletedById(id, timeUtils.getTimeFast())
    }

    fun deleteOld(time: Long) {
        val filesOnDisk = mutableListOf<File>()

        val transactionResult = database.transactional {
            val oldFiles = tempFilesDao.findDeletedOld(time)

            oldFiles.forEach { oldFile ->
                filesOnDisk += oldFile.asFile()
                tempFilesDao.deleteForReal(oldFile.id!!)
            }

            return@transactional true
        }

        if (!transactionResult) {
            Timber.tag(TAG).w("Could not delete from the DB one of the records")
            return
        }

        filesOnDisk.forEach { fileOnDisk ->
            if (fileOnDisk.exists()) {
                if (!fileOnDisk.delete()) {
                    Timber.tag(TAG).w("Could not delete file ${fileOnDisk.absoluteFile}")
                }
            }
        }
    }
}