package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import timber.log.Timber
import java.io.File

/**
 * Created by kirakishou on 3/3/2018.
 */
open class TempFileRepository(
    private val filesDir: String,
    private val database: MyDatabase,
    private val coroutinesPool: CoroutineThreadPoolProvider
) {
    private val tempFileDao = database.tempFileDao()

    suspend fun init() {
        createTempFilesDirIfNotExists()
    }

    suspend fun createTempFile(photoOwnerId: Long): TempFileEntity {
        val file = createFile()
        if (!file.exists()) {
            return TempFileEntity.empty()
        }

        try {
            val tempFileEntity = TempFileEntity.create(photoOwnerId, file.absolutePath)
            val id = tempFileDao.insert(tempFileEntity)

            if (id <= 0L) {
                deleteFileIfExists(file)
                return TempFileEntity.empty()
            }

            return tempFileEntity
                .also { it.id = id }
                .also { it.photoOwnerId = photoOwnerId }
        } catch (error: Throwable) {
            Timber.e(error)
            deleteFileIfExists(file)
            return TempFileEntity.empty()
        }
    }

    suspend fun findById(id: Long): TempFileEntity {
        return tempFileDao.findById(id) ?: TempFileEntity.empty()
    }

    suspend fun findAll(): List<TempFileEntity> {
        return tempFileDao.findAll()
    }

    suspend fun deleteById(id: Long): Boolean {
        val tempFileEntity = tempFileDao.findById(id)
            ?: return true

        deleteFileIfExists(File(tempFileEntity.filePath))
        return tempFileDao.deleteById(id) > 0
    }

    private fun createTempFilesDirIfNotExists() {
        val fullPathFile = File(filesDir)
        Timber.d(fullPathFile.absolutePath)

        if (!fullPathFile.exists()) {
            if (!fullPathFile.mkdirs()) {
                Timber.e("Could not create directory ${fullPathFile.absolutePath}")
            }
        }
    }

    private fun createFile(): File { filesDir
        val fullPathFile = File(filesDir)
        return File.createTempFile("file_", ".tmp", fullPathFile)
    }

    private fun deleteFileIfExists(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }
}