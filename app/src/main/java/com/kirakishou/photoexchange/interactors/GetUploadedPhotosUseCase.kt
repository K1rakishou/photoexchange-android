package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GetUploadedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GetUploadedPhotosUseCase(
  private val settingsRepository: SettingsRepository,
  private val getUploadedPhotosRepository: GetUploadedPhotosRepository,
  private val timeUtils: TimeUtils,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetUploadedPhotosUseCase"

  open suspend fun loadPageOfPhotos(
    lastUploadedOn: Long,
    count: Int
  ): Paged<UploadedPhoto> {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("loadPageOfPhotos called")

      val (time, userId) = getParameters(lastUploadedOn)
      return@withContext getUploadedPhotosRepository.getPage(time, count, userId)
    }
  }

  open suspend fun loadFreshPhotos(
    lastUploadedOn: Long,
    count: Int
  ): Paged<UploadedPhoto> {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("loadFreshPhotos called")

      val (time, userId) = getParameters(lastUploadedOn)
      return@withContext getUploadedPhotosRepository.getFresh(time, count, userId)
    }
  }

  private suspend fun getParameters(lastUploadedOn: Long): Pair<Long, String> {
    val time = if (lastUploadedOn != -1L) {
      lastUploadedOn
    } else {
      timeUtils.getTimeFast()
    }

    val userId = settingsRepository.getUserId()
    if (userId.isEmpty()) {
      throw EmptyUserIdException()
    }

    return Pair(time, userId)
  }
}