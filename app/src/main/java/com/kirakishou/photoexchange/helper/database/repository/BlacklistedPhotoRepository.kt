package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.source.local.BlacklistedPhotoLocalSource
import kotlinx.coroutines.withContext

class BlacklistedPhotoRepository(
  private val blacklistedPhotoLocalSource: BlacklistedPhotoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {

  suspend fun blacklist(photoName: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext blacklistedPhotoLocalSource.blacklist(photoName)
    }
  }

  suspend fun isBlacklisted(photoName: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext blacklistedPhotoLocalSource.isBlacklisted(photoName)
    }
  }

  suspend fun <T> filterBlacklistedPhotos(photos: List<T>, nameSelector: (T) -> String): List<T> {
    return withContext(coroutineContext) {
      return@withContext blacklistedPhotoLocalSource.filterBlacklistedPhotos(photos, nameSelector)
    }
  }

}