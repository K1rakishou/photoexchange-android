package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.source.local.BlacklistedPhotoLocalSource

class BlacklistedPhotoRepository(
  private val blacklistedPhotoLocalSource: BlacklistedPhotoLocalSource
) : BaseRepository() {

  suspend fun blacklist(photoName: String): Boolean {
    return blacklistedPhotoLocalSource.blacklist(photoName)
  }

  suspend fun isBlacklisted(photoName: String): Boolean {
    return blacklistedPhotoLocalSource.isBlacklisted(photoName)
  }

  suspend fun <T> filterBlacklistedPhotos(photos: List<T>, nameSelector: (T) -> String): List<T> {
    return blacklistedPhotoLocalSource.filterBlacklistedPhotos(photos, nameSelector)
  }

}