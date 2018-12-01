package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GetReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GetReceivedPhotosUseCase(
  private val settingsRepository: SettingsRepository,
  private val getReceivedPhotosRepository: GetReceivedPhotosRepository,
  private val timeUtils: TimeUtils,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetReceivedPhotosUseCase"

  open suspend fun loadPageOfPhotos(
    lastUploadedOn: Long,
    count: Int
  ): Either<Exception, List<ReceivedPhoto>> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        Timber.tag(TAG).d("loadPageOfPhotos called")

        val (time, userId) = getParameters(lastUploadedOn)
        return@myRunCatching getReceivedPhotosRepository.getPage(userId, time, count)
      }
    }
  }

  open suspend fun loadFreshPhotos(
    lastUploadedOn: Long,
    count: Int
  ): Either<Exception, List<ReceivedPhoto>> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        Timber.tag(TAG).d("loadPageOfPhotos called")

        val (time, userId) = getParameters(lastUploadedOn)
        return@myRunCatching getReceivedPhotosRepository.getFresh(userId, time, count)
      }
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