package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.SettingEntity
import com.kirakishou.photoexchange.mvp.model.exception.EmptyUserIdException
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
      return@withContext settingsDao.findByName(USER_ID_SETTING)?.settingValue
        ?: ""
    }
  }

  open suspend fun getUserIdOrThrow(): String {
    return withContext(coroutineContext) {
      return@withContext settingsDao.findByName(USER_ID_SETTING)?.settingValue
        ?: throw EmptyUserIdException()
    }
  }

  suspend fun saveMakePublicFlag(makePublic: Boolean?) {
    withContext(coroutineContext) {
      val value = MakePhotosPublicState.fromBoolean(makePublic).value.toString()
      settingsDao.insert(SettingEntity(MAKE_PHOTOS_PUBLIC_SETTING, value))
    }
  }

  suspend fun getMakePublicFlag(): MakePhotosPublicState {
    return withContext(coroutineContext) {
      val result = settingsDao.findByName(MAKE_PHOTOS_PUBLIC_SETTING)
        ?: return@withContext MakePhotosPublicState.Neither

      return@withContext MakePhotosPublicState.fromInt(result.settingValue?.toInt())
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

  enum class MakePhotosPublicState(val value: Int) {
    AlwaysPublic(0),
    AlwaysPrivate(1),
    Neither(2);

    companion object {
      fun fromBoolean(makePublic: Boolean?): MakePhotosPublicState {
        return when (makePublic) {
          null -> Neither
          true -> AlwaysPublic
          false -> AlwaysPrivate
        }
      }

      fun fromInt(value: Int?): MakePhotosPublicState {
        return when (value) {
          0 -> AlwaysPublic
          1 -> AlwaysPrivate
          else -> Neither
        }
      }
    }
  }

  companion object {
    const val USER_ID_SETTING = "USER_ID"
    const val MAKE_PHOTOS_PUBLIC_SETTING = "MAKE_PHOTOS_PUBLIC"
    const val GPS_PERMISSION_GRANTED_SETTING = "GPS_PERMISSION_GRANTED"
  }
}