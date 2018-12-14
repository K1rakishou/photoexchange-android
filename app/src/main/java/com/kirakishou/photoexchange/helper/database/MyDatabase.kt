package com.kirakishou.photoexchange.helper.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.dao.*
import com.kirakishou.photoexchange.helper.database.entity.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Created by kirakishou on 9/12/2017.
 */

@Database(entities = [
  TakenPhotoEntity::class,
  TempFileEntity::class,
  SettingEntity::class,
  ReceivedPhotoEntity::class,
  GalleryPhotoEntity::class,
  UploadedPhotoEntity::class,
  BlacklistedPhotoEntity::class,
  PhotoAdditionalInfoEntity::class
], version = 1)
abstract class MyDatabase : RoomDatabase() {
  abstract fun takenPhotoDao(): TakenPhotoDao
  abstract fun tempFileDao(): TempFileDao
  abstract fun settingsDao(): SettingsDao
  abstract fun receivedPhotoDao(): ReceivedPhotoDao
  abstract fun galleryPhotoDao(): GalleryPhotoDao
  abstract fun uploadedPhotoDao(): UploadedPhotoDao
  abstract fun blacklistedPhotoDao(): BlacklistedPhotoDao
  abstract fun photoAdditionalInfoDao(): PhotoAdditionalInfoDao

  open suspend fun <T> transactional(func: suspend () -> T): T {
    beginTransaction()

    try {
      val result = func()
      setTransactionSuccessful()

      return result
    } finally {
      endTransaction()
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