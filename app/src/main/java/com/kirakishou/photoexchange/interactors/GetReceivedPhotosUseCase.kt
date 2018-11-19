package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.DatabaseException
import kotlinx.coroutines.withContext
import net.response.GetReceivedPhotosResponse
import timber.log.Timber

open class GetReceivedPhotosUseCase(
  private val database: MyDatabase,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val apiClient: ApiClient,
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

        receivedPhotosRepository.deleteOldPhotos()

        val pageOfReceivedPhotos = receivedPhotosRepository.getPageOfReceivedPhotos(time, count)
        if (pageOfReceivedPhotos.size == count) {
          return@myRunCatching pageOfReceivedPhotos
        }

        val receivedPhotos = apiClient.getReceivedPhotos(userId, lastUploadedOn, count)
        if (receivedPhotos.isEmpty()) {
          return@myRunCatching emptyList<ReceivedPhoto>()
        }

        val transactionResult = storeInDatabase(receivedPhotos)
        if (!transactionResult) {
          throw DatabaseException("Could not cache received photos in the database")
        }

        val sorted = receivedPhotos
          .sortedByDescending { it.photoId }

        return@myRunCatching ReceivedPhotosMapper.FromResponse.GetReceivedPhotos.toReceivedPhotos(sorted)
      }
    }
  }

  private suspend fun storeInDatabase(receivedPhotos: List<GetReceivedPhotosResponse.ReceivedPhotoResponseData>): Boolean {
    return database.transactional {
      for (receivedPhoto in receivedPhotos) {
        if (!uploadedPhotosRepository.updateReceiverInfo(receivedPhoto.uploadedPhotoName)) {
          return@transactional false
        }
      }

      if (!receivedPhotosRepository.saveMany(receivedPhotos)) {
        return@transactional false
      }

      return@transactional true
    }
  }
}