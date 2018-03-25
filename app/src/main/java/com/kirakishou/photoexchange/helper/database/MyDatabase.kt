package com.kirakishou.photoexchange.helper.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.kirakishou.photoexchange.helper.database.dao.MyPhotoDao
import com.kirakishou.photoexchange.helper.database.dao.SettingsDao
import com.kirakishou.photoexchange.helper.database.dao.TempFileDao
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.SettingEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity

/**
 * Created by kirakishou on 9/12/2017.
 */

@Database(entities = [
    MyPhotoEntity::class,
    TempFileEntity::class,
    SettingEntity::class
], version = 1)
abstract class MyDatabase : RoomDatabase() {

    abstract fun myPhotoDao(): MyPhotoDao
    abstract fun tempFileDao(): TempFileDao
    abstract fun settingsDao(): SettingsDao

    inline fun transactional(func: () -> Boolean) {
        this.beginTransaction()

        try {
            if (func()) {
                this.setTransactionSuccessful()
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