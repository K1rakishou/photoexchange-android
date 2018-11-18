package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import kotlinx.coroutines.withContext
import java.lang.Exception

open class ReportPhotoUseCase(
  private val apiClient: ApiClient,
  private val galleryPhotoRepository: GalleryPhotoRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "ReportPhotoUseCase"

  suspend fun reportPhoto(userId: String, photoName: String): Either<Exception, Boolean> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val isReported = apiClient.reportPhoto(userId, photoName)
        val galleryPhoto = galleryPhotoRepository.findByPhotoName(photoName)

        if (galleryPhoto != null) {
          galleryPhotoRepository.reportPhoto(galleryPhoto.galleryPhotoId, isReported)
        }

        return@myRunCatching isReported
      }
    }
  }
}