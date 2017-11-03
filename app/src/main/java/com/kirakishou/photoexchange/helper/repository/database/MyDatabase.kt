package com.kirakishou.photoexchange.helper.repository.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

/**
 * Created by kirakishou on 9/12/2017.
 */

/*@Database(entities = arrayOf(), version = 1)
abstract class MyDatabase : RoomDatabase() {

    fun runInTransaction(func: () -> Unit) {
        this.beginTransaction()

        try {
            func()
            this.setTransactionSuccessful()
        } finally {
            this.endTransaction()
        }
    }
}*/