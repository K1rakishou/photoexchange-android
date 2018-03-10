package com.kirakishou.photoexchange.helper.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.kirakishou.photoexchange.helper.database.dao.MyPhotoDao
import com.kirakishou.photoexchange.helper.database.dao.TempFileDao
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity

/**
 * Created by kirakishou on 9/12/2017.
 */

@Database(entities = [
    MyPhotoEntity::class,
    TempFileEntity::class], version = 1)
abstract class MyDatabase : RoomDatabase() {

    abstract fun myPhotoDao(): MyPhotoDao
    abstract fun tempFileDao(): TempFileDao

    suspend fun <T> transactional(func: suspend () -> TransactionResult<T>): T {
        this.beginTransaction()

        try {
            val transactionResult = func()
            return when (transactionResult) {
                is TransactionResult.Fail -> {
                    transactionResult.result
                }

                is TransactionResult.Success -> {
                    this.setTransactionSuccessful()
                    transactionResult.result
                }
            }
        } finally {
            this.endTransaction()
        }
    }

    companion object {
        const val SQLITE_TRUE = 1
        const val SQLITE_FALSE = 0
    }
}