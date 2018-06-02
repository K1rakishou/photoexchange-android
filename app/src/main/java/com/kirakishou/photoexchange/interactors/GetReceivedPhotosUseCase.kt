package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.rx2.rxSingle
import timber.log.Timber

class GetReceivedPhotosUseCase(
    private val database: MyDatabase,
    private val receivedPhotosRepository: ReceivedPhotosRepository,
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val apiClient: ApiClient
) {

    private val TAG = "GetReceivedPhotosUseCase"

    fun loadPageOfPhotos(userId: String, lastId: Long, count: Int): Single<Either<ErrorCode.GetReceivedPhotosErrors, List<ReceivedPhoto>>> {
        return rxSingle {
            try {
                Timber.tag(TAG).d("sending loadPageOfPhotos request...")

                val response = apiClient.getReceivedPhotoIds(userId, lastId, count).await()
                val errorCode = response.errorCode as ErrorCode.GetReceivedPhotosErrors

                if (errorCode !is ErrorCode.GetReceivedPhotosErrors.Ok) {
                    return@rxSingle Either.Error(errorCode)
                }

                val photosResultList = mutableListOf<ReceivedPhoto>()
                val receivedPhotoIds = response.receivedPhotoIds
                if (receivedPhotoIds.isEmpty()) {
                    return@rxSingle Either.Value(photosResultList)
                }

                val receivedPhotosFromDb = receivedPhotosRepository.findMany(receivedPhotoIds)
                val photoIdsToGetFromServer = Utils.filterListAlreadyContaning(receivedPhotoIds, receivedPhotosFromDb.map { it.photoId })
                photosResultList += receivedPhotosFromDb

                if (photoIdsToGetFromServer.isNotEmpty()) {
                    val result = getFreshPhotosFromServer(userId, photoIdsToGetFromServer)
                    if (result is Either.Error) {
                        Timber.tag(TAG).w("Could not get fresh photos from the server, errorCode = ${result.error}")
                        return@rxSingle Either.Error(result.error)
                    }

                    (result as Either.Value)

                    Timber.tag(TAG).d("Fresh gallery photo ids = ${result.value.map { it.photoId }}")
                    photosResultList += result.value
                }

                photosResultList.sortBy { it.photoId }
                return@rxSingle Either.Value(photosResultList)
            } catch (error: Throwable) {
                Timber.tag(TAG).e(error)
                return@rxSingle Either.Error(ErrorCode.GetReceivedPhotosErrors.UnknownError())
            }
        }
    }

    private suspend fun getFreshPhotosFromServer(userId: String, photoIds: List<Long>): Either<ErrorCode.GetReceivedPhotosErrors, List<ReceivedPhoto>> {
        val photoIdsToBeRequested = photoIds.joinToString(Constants.PHOTOS_DELIMITER)

        val response = apiClient.getReceivedPhotos(userId, photoIdsToBeRequested).await()
        val errorCode = response.errorCode as ErrorCode.GetReceivedPhotosErrors

        if (errorCode !is ErrorCode.GetReceivedPhotosErrors.Ok) {
            return Either.Error(errorCode)
        }

        if (response.receivedPhotos.isEmpty()) {
            return Either.Value(emptyList())
        }

        val transactionResult = database.transactional {
            for (receivedPhoto in response.receivedPhotos) {
                if (!uploadedPhotosRepository.updateReceiverInfo(receivedPhoto.uploadedPhotoName)) {
                    return@transactional false
                }
            }

            if (!receivedPhotosRepository.saveMany(response.receivedPhotos)) {
                return@transactional false
            }

            return@transactional true
        }

        if (!transactionResult) {
            return Either.Error(ErrorCode.GetReceivedPhotosErrors.DatabaseError())
        }

        return Either.Value(ReceivedPhotosMapper.FromResponse.GetReceivedPhotos.toReceivedPhotos(response.receivedPhotos))
    }
}