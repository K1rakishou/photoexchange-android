package com.kirakishou.photoexchange.helper.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.kirakishou.photoexchange.helper.database.dao.UploadedPhotosDao
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity

/**
 * Created by kirakishou on 9/12/2017.
 */

@Database(entities = arrayOf(
        UploadedPhotoEntity::class
), version = 1)
abstract class MyDatabase : RoomDatabase() {

    abstract fun uploadedPhotosDao(): UploadedPhotosDao

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