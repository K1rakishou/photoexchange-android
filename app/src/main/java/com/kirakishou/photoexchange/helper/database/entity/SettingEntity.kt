package com.kirakishou.photoexchange.helper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.SettingEntity.Companion.TABLE_NAME

/**
 * Created by kirakishou on 3/17/2018.
 */

@Entity(tableName = TABLE_NAME)
class SettingEntity(

  @PrimaryKey
  @ColumnInfo(name = SETTING_NAME_COLUMN)
  var settingName: String = "",

  @ColumnInfo(name = SETTING_VALUE_COLUMN)
  var settingValue: String? = null
) {

  companion object {
    const val TABLE_NAME = "SETTINGS"

    const val SETTING_NAME_COLUMN = "SETTING_NAME"
    const val SETTING_VALUE_COLUMN = "SETTING_VALUE"
  }
}