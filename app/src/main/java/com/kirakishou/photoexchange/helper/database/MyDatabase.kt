package com.kirakishou.photoexchange.helper.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.kirakishou.photoexchange.helper.database.dao.PhotoAnswerDao
import com.kirakishou.photoexchange.helper.database.dao.TakenPhotosDao
import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity

/**
 * Created by kirakishou on 9/12/2017.
 */

@Database(entities = arrayOf(
        TakenPhotoEntity::class,
        PhotoAnswerEntity::class
), version = 1)
abstract class MyDatabase : RoomDatabase() {

    abstract fun takenPhotosDao(): TakenPhotosDao
    abstract fun photoAnswerDao(): PhotoAnswerDao

    fun runInTransaction(func: () -> Unit) {
        this.beginTransaction()

        try {
            func()
            this.setTransactionSuccessful()
        } finally {
            this.endTransaction()
        }
    }

    companion object {
        const val SQLITE_TRUE = 1
        const val SQLITE_FALSE = 0
    }
}