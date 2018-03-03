package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * Created by kirakishou on 3/3/2018.
 */
class MyPhotoRepository(
    private val database: MyDatabase,
    private val coroutinesPool: CoroutineThreadPoolProvider
) {
    private val takenPhotoDao = database.takenPhotoDao()

    fun insert(myPhotoEntity: MyPhotoEntity): Deferred<Boolean> {
        return async(coroutinesPool.provideDb()) {
            return@async takenPhotoDao.insert(myPhotoEntity) > 0L
        }
    }

    fun findById(id: Long): Deferred<MyPhotoEntity?> {
        return async(coroutinesPool.provideDb()) {
            return@async takenPhotoDao.findById(id)
        }
    }
}