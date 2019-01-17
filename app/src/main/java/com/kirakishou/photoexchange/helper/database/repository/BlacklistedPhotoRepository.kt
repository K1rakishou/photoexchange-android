package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.source.local.BlacklistedPhotoLocalSource
import timber.log.Timber

class BlacklistedPhotoRepository(
  private val blacklistedPhotoLocalSource: BlacklistedPhotoLocalSource
) : BaseRepository() {
  private val TAG = "BlacklistedPhotoRepository"

  suspend fun blacklist(photoName: String): Boolean {
    return blacklistedPhotoLocalSource.blacklist(photoName)
  }

  suspend fun <T> filterBlacklistedPhotos(photos: List<T>, nameSelector: (T) -> String): List<T> {
    if (photos.isEmpty()) {
      return emptyList()
    }

    val resultList = mutableListOf<T>()

    for (photo in photos) {
      //TODO: make this faster by checking blacklisted photos in batches
      if (blacklistedPhotoLocalSource.isBlacklisted(nameSelector(photo))) {
        continue
      }

      resultList += photo
    }

    Timber.tag(TAG).d("Filtered ${photos.size - resultList.size} photos")
    return resultList
  }

  suspend fun deleteOld() {
    blacklistedPhotoLocalSource.deleteOld()
  }

}