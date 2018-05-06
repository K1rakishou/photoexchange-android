package com.kirakishou.photoexchange.helper.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.kirakishou.photoexchange.helper.database.dao.*
import com.kirakishou.photoexchange.helper.database.entity.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Created by kirakishou on 9/12/2017.
 */

@Database(entities = [
    MyPhotoEntity::class,
    TempFileEntity::class,
    SettingEntity::class,
    PhotoAnswerEntity::class,
    GalleryPhotoEntity::class
], version = 1)
abstract class MyDatabase : RoomDatabase() {

    val dbLock = ReentrantLock()

    abstract fun myPhotoDao(): MyPhotoDao
    abstract fun tempFileDao(): TempFileDao
    abstract fun settingsDao(): SettingsDao
    abstract fun photoAnswerDao(): PhotoAnswerDao
    abstract fun galleryPhotoDao(): GalleryPhotoDao

    inline fun transactional(func: () -> Boolean): Boolean {
        dbLock.withLock {
            this.beginTransaction()

            try {
                val result = func()
                if (result) {
                    this.setTransactionSuccessful()
                }

                return result
            } finally {
                this.endTransaction()
            }
        }
    }

    companion object {
        const val SQLITE_TRUE = 1
        const val SQLITE_FALSE = 0
    }
}

internal fun Long.isFail(): Boolean {
    return this <= 0L
}

internal fun Long.isSuccess(): Boolean {
    return this > 0L
}

internal fun Int.isFail(): Boolean {
    return this <= 0L
}

internal fun Int.isSuccess(): Boolean {
    return this > 0L
}