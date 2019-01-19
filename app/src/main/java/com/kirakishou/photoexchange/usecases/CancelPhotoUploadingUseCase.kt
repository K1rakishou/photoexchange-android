package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import kotlinx.coroutines.withContext

open class CancelPhotoUploadingUseCase(
  private val takenPhotosRepository: TakenPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  suspend fun cancelPhotoUploading(photoId: Long) {
    withContext(coroutineContext) {
      if (takenPhotosRepository.findById(photoId) == null) {
        return@withContext
      }

      if (!takenPhotosRepository.deletePhotoById(photoId)) {
        throw DatabaseException("Could not delete photo with id ${photoId}")
      }
    }
  }

}