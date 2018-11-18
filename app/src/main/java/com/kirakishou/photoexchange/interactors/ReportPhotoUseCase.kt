package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.mvp.model.exception.ReportPhotoExceptions
import kotlinx.coroutines.withContext

open class ReportPhotoUseCase(
  private val apiClient: ApiClient,
  private val galleryPhotoRepository: GalleryPhotoRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "ReportPhotoUseCase"

  suspend fun reportPhoto(userId: String, photoName: String): Either<ReportPhotoExceptions, Boolean> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching<ReportPhotoExceptions, Boolean> {
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