package com.kirakishou.photoexchange.helper.repository.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.kirakishou.photoexchange.helper.repository.dao.SentPhotosDao
import com.kirakishou.photoexchange.helper.repository.entity.SentPhotoEntity

/**
 * Created by kirakishou on 9/12/2017.
 */

@Database(entities = arrayOf(SentPhotoEntity::class), version = 1)
abstract class MyDatabase : RoomDatabase() {

    abstract fun sentPhotosDao(): SentPhotosDao

    fun runInTransaction(func: () -> Unit) {
        this.beginTransaction()

        try {
            func()
            this.setTransactionSuccessful()
        } finally {
            this.endTransaction()
        }
    }
}