package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.BlacklistedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.util.TimeUtils
import timber.log.Timber

open class BlacklistedPhotoLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val blacklistedEarlierThanTimeDelta: Long
) {
  private val TAG = "BlacklistedPhotoLocalSource"
  private val blacklistedPhotoDao = database.blacklistedPhotoDao()

  fun blacklist(photoName: String): Boolean {
    val now = timeUtils.getTimeFast()
    val entity = BlacklistedPhotoEntity.create(photoName, now)

    return blacklistedPhotoDao.save(entity).isSuccess()
  }

  fun isBlacklisted(photoName: String): Boolean {
    return blacklistedPhotoDao.find(photoName) != null
  }

  fun deleteOld() {
    val now = timeUtils.getTimeFast()
    blacklistedPhotoDao.deleteOlderThan(now - blacklistedEarlierThanTimeDelta)

    Timber.tag(TAG).d("deleteOld called")
  }

}