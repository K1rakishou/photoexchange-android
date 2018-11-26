package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GetGalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GetGalleryPhotosUseCase(
  private val settingsRepository: SettingsRepository,
  private val getGalleryPhotosRepository: GetGalleryPhotosRepository,
  private val timeUtils: TimeUtils,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetGalleryPhotosUseCase"

  open suspend fun loadPageOfPhotos(
    lastUploadedOn: Long,
    count: Int
  ): Either<Exception, List<GalleryPhoto>> {
    Timber.tag(TAG).d("sending loadPageOfPhotos request...")

    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val time = if (lastUploadedOn != -1L) {
          lastUploadedOn
        } else {
          timeUtils.getTimeFast()
        }

        val userId = settingsRepository.getUserId()
        if (userId.isEmpty()) {
          throw EmptyUserIdException()
        }

        return@myRunCatching getGalleryPhotosRepository.getPage(userId, time, count)
      }
    }
  }
}