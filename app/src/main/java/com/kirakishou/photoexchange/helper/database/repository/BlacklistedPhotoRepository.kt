package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.source.local.BlacklistedPhotoLocalSource

class BlacklistedPhotoRepository(
  private val blacklistedPhotoLocalSource: BlacklistedPhotoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {

  fun ban(photoName: String): Boolean {
    return blacklistedPhotoLocalSource.ban(photoName)
  }

  fun isBanned(photoName: String): Boolean {
    return blacklistedPhotoLocalSource.isBanned(photoName)
  }

}