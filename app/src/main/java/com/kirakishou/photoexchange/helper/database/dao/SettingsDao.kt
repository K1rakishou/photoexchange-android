package com.kirakishou.photoexchange.helper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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