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

        return createInternal(file)
    }

    private fun createInternal(file: File): TempFileEntity {
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
            deleteFileFromDisk(fileOnDisk)
        }
    }

    fun deleteEmptyTempFiles() {
        val allEmptyFiles = tempFilesDao.findAllEmpty()

        allEmptyFiles.forEach {
            //do not delete file record from the database if we could not delete the file from the disk first
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