package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import java.io.File

/**
 * Created by kirakishou on 3/3/2018.
 */
class TempFileRepository(
    private val database: MyDatabase,
    private val coroutinesPool: CoroutineThreadPoolProvider
) {
    private val tempFileDao = database.tempFileDao()

    fun createTempFile(): Deferred<TempFileEntity?> {
        return async(coroutinesPool.provideDb()) {
            val file = File.createTempFile("file", "tmp")
            if (!file.exists()) {
                return@async null
            }

            try {
                val tempFileEntity = TempFileEntity.create(file.absolutePath)
                if (tempFileDao.insert(tempFileEntity) <= 0L) {
                    deleteFileIfExists(file)
                    return@async null
                }

                return@async tempFileEntity
            } catch (error: Throwable) {
                deleteFileIfExists(file)
                return@async null
            }
        }
    }

    private fun deleteFileIfExists(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }
}