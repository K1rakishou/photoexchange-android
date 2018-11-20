package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GetGalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.ReportPhotoRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import kotlinx.coroutines.withContext
import java.lang.Exception

open class ReportPhotoUseCase(
  private val reportPhotoRepository: ReportPhotoRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "ReportPhotoUseCase"

  suspend fun reportPhoto(userId: String, photoName: String): Either<Exception, Boolean> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        return@myRunCatching reportPhotoRepository.reportPhoto(userId, photoName)
      }
    }
  }
}