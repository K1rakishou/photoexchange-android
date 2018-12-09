package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import kotlinx.coroutines.withContext

class BlacklistPhotoUseCase(
  private val database: MyDatabase,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val galleryPhotosRepository: GalleryPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  suspend fun blacklistPhoto(photoName: String) {
    withContext(coroutineContext) {
      database.transactional {
        //TODO: add photo name to banned photos table

        receivedPhotosRepository.deleteByPhotoName(photoName)
        galleryPhotosRepository.deleteByPhotoName(photoName)
      }
    }
  }

}