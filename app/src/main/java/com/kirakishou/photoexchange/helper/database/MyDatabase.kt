package com.kirakishou.photoexchange.helper.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.dao.*
import com.kirakishou.photoexchange.helper.database.entity.*
import kotlinx.coroutines.withContext
import timber.log.Timber

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
  abstract fun takenPhotoDao(): TakenPhotoDao
  abstract fun tempFileDao(): TempFileDao
  abstract fun settingsDao(): SettingsDao
  abstract fun receivedPhotoDao(): ReceivedPhotoDao
  abstract fun galleryPhotoDao(): GalleryPhotoDao
  abstract fun galleryPhotoInfoDao(): GalleryPhotoInfoDao
  abstract fun uploadedPhotoDao(): UploadedPhotoDao

  lateinit var dispatchersProvider: DispatchersProvider

  open suspend fun transactional(func: suspend () -> Boolean): Boolean {
    return withContext(dispatchersProvider.DB()) {
      Timber.d("before beginTransaction")
      beginTransaction()
      Timber.d("after beginTransaction")

      try {
        val result = func()
        if (result) {
          setTransactionSuccessful()
        }

        return@withContext result
      } finally {
        Timber.d("before endTransaction")
        endTransaction()
        Timber.d("after endTransaction")
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