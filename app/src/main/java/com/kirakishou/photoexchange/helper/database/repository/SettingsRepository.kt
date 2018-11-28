package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.PhotosVisibility
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.SettingEntity
import kotlinx.coroutines.withContext

/**
 * Created by kirakishou on 3/17/2018.
 */
open class SettingsRepository(
  private val database: MyDatabase,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val settingsDao = database.settingsDao()

  suspend fun saveUserId(userId: String?): Boolean {
    return withContext(coroutineContext) {
      return@withContext settingsDao.insert(SettingEntity(USER_ID_SETTING, userId)) > 0
    }
  }

  open suspend fun getUserId(): String {
    return withContext(coroutineContext) {
      return@withContext settingsDao.findByName(USER_ID_SETTING)?.settingValue ?: ""
    }
  }

  open suspend fun saveFirebaseToken(token: String?): Boolean {
    return withContext(coroutineContext) {
      return@withContext settingsDao.insert(SettingEntity(FIREBASE_TOKEN, token)) > 0
    }
  }

  open suspend fun getFirebaseToken(): String {
    return withContext(coroutineContext) {
      return@withContext settingsDao.findByName(FIREBASE_TOKEN)?.settingValue ?: ""
    }
  }

  suspend fun saveMakePublicFlag(makePublic: Boolean?) {
    withContext(coroutineContext) {
      val value = PhotosVisibility.fromBoolean(makePublic).value.toString()
      settingsDao.insert(SettingEntity(MAKE_PHOTOS_PUBLIC_SETTING, value))
    }
  }

  suspend fun getMakePublicFlag(): PhotosVisibility {
    return withContext(coroutineContext) {
      val result = settingsDao.findByName(MAKE_PHOTOS_PUBLIC_SETTING)
        ?: return@withContext PhotosVisibility.Neither

      return@withContext PhotosVisibility.fromInt(result.settingValue?.toInt())
    }
  }

  suspend fun updateGpsPermissionGranted(granted: Boolean) {
    withContext(coroutineContext) {
      settingsDao.insert(SettingEntity(GPS_PERMISSION_GRANTED_SETTING, granted.toString()))
    }
  }

  suspend fun isGpsPermissionGranted(): Boolean {
    return withContext(coroutineContext) {
      return@withContext settingsDao.findByName(GPS_PERMISSION_GRANTED_SETTING)?.settingValue?.toBoolean()
        ?: false
    }
  }

  companion object {
    const val USER_ID_SETTING = "USER_ID"
    const val FIREBASE_TOKEN = "FIREBASE_TOKEN"
    const val MAKE_PHOTOS_PUBLIC_SETTING = "MAKE_PHOTOS_PUBLIC"
    const val GPS_PERMISSION_GRANTED_SETTING = "GPS_PERMISSION_GRANTED"
  }
}