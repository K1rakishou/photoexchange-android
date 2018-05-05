package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.SettingEntity
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mvp.model.other.LonLat

/**
 * Created by kirakishou on 3/17/2018.
 */
class SettingsRepository(
    private val database: MyDatabase
) {
    private val settingsDao = database.settingsDao()

    fun generateUserIdIfNotExists() {
        if (getUserId() == null) {
            saveUserId(Utils.generateUserId())
        }
    }

    fun saveUserId(userId: String?): Boolean {
        return settingsDao.insert(SettingEntity(USER_ID_SETTING, userId)) > 0
    }

    fun getUserId(): String? {
        return settingsDao.findByName(USER_ID_SETTING)?.settingValue
    }

    fun saveMakePublicFlag(makePublic: Boolean?) {
        val value = MakePhotosPublicState.fromBoolean(makePublic).value.toString()
        settingsDao.insert(SettingEntity(MAKE_PHOTOS_PUBLIC_SETTING, value))
    }

    fun getMakePublicFlag(): MakePhotosPublicState {
        val result = settingsDao.findByName(MAKE_PHOTOS_PUBLIC_SETTING)
            ?: return MakePhotosPublicState.Neither

        return MakePhotosPublicState.fromInt(result.settingValue?.toInt())
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
    }
}