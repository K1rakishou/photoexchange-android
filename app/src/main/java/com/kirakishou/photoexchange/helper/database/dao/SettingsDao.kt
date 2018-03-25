package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.entity.SettingEntity

/**
 * Created by kirakishou on 3/17/2018.
 */

@Dao
abstract class SettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(settingEntity: SettingEntity): Long

    @Query("SELECT * FROM ${SettingEntity.TABLE_NAME} " +
        "WHERE ${SettingEntity.SETTING_NAME_COLUMN} = :settingName LIMIT 1")
    abstract fun findByName(settingName: String): SettingEntity?
}