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
    TakenPhotoEntity::class,
    TempFileEntity::class,
    SettingEntity::class,
    ReceivedPhotoEntity::class,
    GalleryPhotoEntity::class,
    GalleryPhotoInfoEntity::class,
    UploadedPhotoEntity::class
], version = 1)
abstract class MyDatabase : RoomDatabase() {

    val dbLock = ReentrantLock()

    abstract fun takenPhotoDao(): TakenPhotoDao
    abstract fun tempFileDao(): TempFileDao
    abstract fun settingsDao(): SettingsDao
    abstract fun receivedPhotoDao(): ReceivedPhotoDao
    abstract fun galleryPhotoDao(): GalleryPhotoDao
    abstract fun galleryPhotoInfoDao(): GalleryPhotoInfoDao
    abstract fun uploadedPhotoDao(): UploadedPhotoDao

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