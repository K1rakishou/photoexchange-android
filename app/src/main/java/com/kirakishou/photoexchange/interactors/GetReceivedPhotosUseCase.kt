package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.GetReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.DatabaseException
import kotlinx.coroutines.withContext
import net.response.ReceivedPhotosResponse
import timber.log.Timber

open class GetReceivedPhotosUseCase(
  private val getReceivedPhotosRepository: GetReceivedPhotosRepository,
  private val timeUtils: TimeUtils,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  private val TAG = "GetReceivedPhotosUseCase"

  suspend fun loadPageOfPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): Either<Exception, List<ReceivedPhoto>> {
    Timber.tag(TAG).d("sending loadPageOfPhotos request...")

    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val time = if (lastUploadedOn != -1L) {
          lastUploadedOn
        } else {
          timeUtils.getTimeFast()
        }

        return@myRunCatching getReceivedPhotosRepository.getPage(userId, time, count)
      }
    }
  }
}