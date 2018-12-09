package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.BlacklistedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.util.TimeUtils

open class BlacklistedPhotoLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils
) {
  private val blacklistedPhotoDao = database.blacklistedPhotoDao()

  fun ban(photoName: String): Boolean {
    val now = timeUtils.getTimeFast()
    val entity = BlacklistedPhotoEntity.create(photoName, now)

    return blacklistedPhotoDao.save(entity).isSuccess()
  }

  fun isBanned(photoName: String): Boolean {
    return blacklistedPhotoDao.find(photoName) != null
  }

}