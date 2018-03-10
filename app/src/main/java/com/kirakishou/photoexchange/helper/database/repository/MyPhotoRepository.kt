package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.TransactionResult
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.mapper.MyPhotoMapper
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.state.PhotoState
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

    suspend fun insert(myPhotoEntity: MyPhotoEntity): Deferred<MyPhoto> {
        return async(coroutinesPool.provideDb()) {
            return@async database.transactional {
                val myPhotoEntityId = myPhotoDao.insert(myPhotoEntity)
                if (myPhotoEntityId < 0L) {
                    return@transactional TransactionResult.Fail(MyPhoto.empty())
                }

                val tempFileEntity = tempFileRepository.createTempFile(myPhotoEntityId)
                if (tempFileEntity.isEmpty()) {
                    return@transactional TransactionResult.Fail(MyPhoto.empty())
                }

                return@transactional TransactionResult.Success(MyPhotoMapper.toMyPhoto(myPhotoEntityId, myPhotoEntity, tempFileEntity))
            }
        }
    }

    suspend fun findById(id: Long): Deferred<MyPhoto?> {
        return async(coroutinesPool.provideDb()) {
            return@async database.transactional {
                val myPhotoEntity = myPhotoDao.findById(id) ?: MyPhotoEntity.empty()
                val tempFileEntity = tempFileRepository.findById(id)

                return@transactional TransactionResult.Success(MyPhotoMapper.toMyPhoto(id, myPhotoEntity, tempFileEntity))
            }
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
            return@async database.transactional {
                val tempFileEntity = tempFileRepository.findById(id)
                var fileDeleteResult = false

                if (!tempFileEntity.isEmpty()) {
                    val photoFile = File(tempFileEntity.filePath)
                    if (photoFile.exists()) {
                        fileDeleteResult = photoFile.delete()
                    }
                }

                val myPhotoDeleteResult = myPhotoDao.deleteById(id) > 0
                val tempFileDeleteResult = tempFileRepository.deleteById(id)

                val result = myPhotoDeleteResult && tempFileDeleteResult && fileDeleteResult
                if (result) {
                    return@transactional TransactionResult.Success(true)
                }  else {
                    return@transactional TransactionResult.Fail(false)
                }
            }
        }
    }

    //TODO: make faster by deleting entities in batches
    suspend fun deleteAllWithState(photoState: PhotoState): Deferred<Unit> {
        return async(coroutinesPool.provideDb()) {
            return@async database.transactional {
                val myPhotosList = myPhotoDao.findAllWithState(photoState)

                for (myPhoto in myPhotosList) {
                    if (!deleteById(myPhoto.id!!).await()) {
                        return@transactional TransactionResult.Fail(Unit)
                    }
                }

                return@transactional TransactionResult.Success(Unit)
            }
        }
    }

    suspend fun findAll(): Deferred<List<MyPhoto>> {
        return async(coroutinesPool.provideDb()) {
            val allMyPhotos = arrayListOf<MyPhoto>()
            val allMyPhotoEntities = myPhotoDao.findAll()

            for (myPhotoEntity in allMyPhotoEntities) {
                myPhotoEntity.id?.let { myPhotoId ->
                    val tempFile = tempFileRepository.findById(myPhotoId)

                    MyPhotoMapper.toMyPhoto(myPhotoId, myPhotoEntity, tempFile).let { myPhoto ->
                        allMyPhotos += myPhoto
                    }
                }
            }

            return@async allMyPhotos
        }
    }
}