package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.BlacklistedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.util.TimeUtils
import timber.log.Timber

open class BlacklistedPhotoLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils
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

  fun <T> filterBlacklistedPhotos(photos: List<T>, nameSelector: (T) -> String): List<T> {
    val resultList = mutableListOf<T>()

    for (photo in photos) {
      //TODO: make this faster by checking blacklisted photos in batches
      if (isBlacklisted(nameSelector(photo))) {
        continue
      }

      resultList += photo
    }

    Timber.tag(TAG).d("Filtered ${photos.size - resultList.size} photos")
    return resultList
  }

}