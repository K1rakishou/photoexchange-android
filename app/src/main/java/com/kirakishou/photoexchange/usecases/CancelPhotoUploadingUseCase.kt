package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import kotlinx.coroutines.withContext
import timber.log.Timber

open class CancelPhotoUploadingUseCase(
  private val takenPhotosRepository: TakenPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "CancelPhotoUploadingUseCase"

  open suspend fun cancelPhotoUploading(photoId: Long) {
    withContext(coroutineContext) {
      if (takenPhotosRepository.findById(photoId) == null) {
        Timber.tag(TAG).d("Could not find photo with id ($photoId)")
        return@withContext
      }

      if (!takenPhotosRepository.deletePhotoById(photoId)) {
        throw DatabaseException("Could not delete photo with id ${photoId}")
      }
    }
  }

}