package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.BlacklistedPhotoRepository
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import kotlinx.coroutines.withContext

class BlacklistPhotoUseCase(
  private val database: MyDatabase,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val galleryPhotosRepository: GalleryPhotosRepository,
  private val blacklistedPhotoRepository: BlacklistedPhotoRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  //TODO: remove old blacklisted database entries
  suspend fun blacklistPhoto(photoName: String) {
    withContext(coroutineContext) {
      database.transactional {
        if (!blacklistedPhotoRepository.blacklist(photoName)) {
          throw DatabaseException("Could not blacklist photo ${photoName}")
        }

        receivedPhotosRepository.deleteByPhotoName(photoName)
        galleryPhotosRepository.deleteByPhotoName(photoName)
      }
    }
  }

}