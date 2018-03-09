package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.mapper.MyPhotoMapper
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import java.io.File

/**
 * Created by kirakishou on 3/3/2018.
 */
open class MyPhotoRepository(
    private val database: MyDatabase,
    private val tempFileRepository: TempFileRepository,
    private val coroutinesPool: CoroutineThreadPoolProvider
) {
    private val myPhotoDao = database.myPhotoDao()

    suspend fun init() {
        tempFileRepository.init()
    }

    suspend fun insert(myPhotoEntity: MyPhotoEntity): Deferred<MyPhoto?> {
        return async(coroutinesPool.provideDb()) {
            val myPhotoEntityId = myPhotoDao.insert(myPhotoEntity)
            if (myPhotoEntityId < 0L) {
                return@async null
            }

            val tempFileEntity = tempFileRepository.createTempFile(myPhotoEntityId)
            if (tempFileEntity == null) {
                return@async null
            }

            return@async MyPhotoMapper.toMyPhoto(myPhotoEntityId, myPhotoEntity, tempFileEntity)
        }
    }

    suspend fun findById(id: Long): Deferred<MyPhoto?> {
        return async(coroutinesPool.provideDb()) {
            val myPhotoEntity = myPhotoDao.findById(id)
            val tempFileEntity = tempFileRepository.findById(id)

            return@async MyPhotoMapper.toMyPhoto(id, myPhotoEntity, tempFileEntity)
        }
    }

    suspend fun delete(myPhoto: MyPhoto?): Deferred<Boolean> {
        return async(coroutinesPool.provideDb()) {
            if (myPhoto == null) {
                return@async true
            }

            return@async deleteById(myPhoto.id).await()
        }
    }

    suspend fun deleteById(id: Long): Deferred<Boolean> {
        return async(coroutinesPool.provideDb()) {
            val tempFileEntity = tempFileRepository.findById(id)
            var fileDeleteResult = false

            if (tempFileEntity != null) {
                val photoFile = File(tempFileEntity.filePath)
                if (photoFile.exists()) {
                    fileDeleteResult = photoFile.delete()
                }
            }

            val myPhotoDeleteResult = myPhotoDao.deleteById(id) > 0
            val tempFileDeleteResult = tempFileRepository.deleteById(id)

            return@async myPhotoDeleteResult && tempFileDeleteResult && fileDeleteResult
        }
    }

    suspend fun findAll(): Deferred<List<MyPhoto>> {
        return async(coroutinesPool.provideDb()) {
            val allMyPhotos = arrayListOf<MyPhoto>()
            val allMyPhotoEntities = myPhotoDao.findAll()

            for (myPhotoEntity in allMyPhotoEntities) {
                myPhotoEntity.id?.let { myPhotoId ->
                    val tempFile = tempFileRepository.findById(myPhotoId)

                    MyPhotoMapper.toMyPhoto(myPhotoId, myPhotoEntity, tempFile)?.let { myPhoto ->
                        allMyPhotos += myPhoto
                    }
                }
            }

            return@async allMyPhotos
        }
    }
}