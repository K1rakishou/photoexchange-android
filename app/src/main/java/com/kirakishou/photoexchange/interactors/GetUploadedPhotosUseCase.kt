package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.GetUploadedPhotosException
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GetUploadedPhotosUseCase(
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  private val TAG = "GetUploadedPhotosUseCase"

  open suspend fun loadPageOfPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): Either<Exception, List<UploadedPhoto>> {
    Timber.tag(TAG).d("sending loadPageOfPhotos request...")

    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val time = if (lastUploadedOn != -1L) {
          lastUploadedOn
        } else {
          timeUtils.getTimeFast()
        }

        uploadedPhotosRepository.deleteOld()

        val pageOfUploadedPhotos = uploadedPhotosRepository.getPageOfUploadedPhotos(time, count)
        if (pageOfUploadedPhotos.size == count) {
          return@myRunCatching pageOfUploadedPhotos
        }

        val response = apiClient.getPageOfUploadedPhotos(userId, time, count).await()
        val errorCode = response.errorCode as ErrorCode.GetUploadedPhotosErrors

        if (errorCode !is ErrorCode.GetUploadedPhotosErrors.Ok) {
          throw GetUploadedPhotosException.OnKnownError(errorCode)
        }

        if (response.uploadedPhotos.isEmpty()) {
          return@myRunCatching emptyList<UploadedPhoto>()
        }

        if (!uploadedPhotosRepository.saveMany(response.uploadedPhotos)) {
          throw GetUploadedPhotosException.OnKnownError(ErrorCode.GetUploadedPhotosErrors.DatabaseError())
        }

        return@myRunCatching UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhotos(response.uploadedPhotos)
      }
    }
  }
}