package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GetReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GetReceivedPhotosUseCase(
  private val settingsRepository: SettingsRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val getReceivedPhotosRepository: GetReceivedPhotosRepository,
  private val timeUtils: TimeUtils,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetReceivedPhotosUseCase"

  open suspend fun loadPageOfPhotos(
    forced: Boolean,
    firstUploadedOn: Long,
    lastUploadedOnParam: Long,
    count: Int
  ): Paged<ReceivedPhoto> {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("loadFreshPhotos called")
      val (lastUploadedOn, userId) = getParameters(lastUploadedOnParam)

      receivedPhotosRepository.deleteOldPhotos()
      return@withContext getReceivedPhotosRepository.getPage(
        forced,
        firstUploadedOn,
        lastUploadedOn,
        userId,
        count
      )
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