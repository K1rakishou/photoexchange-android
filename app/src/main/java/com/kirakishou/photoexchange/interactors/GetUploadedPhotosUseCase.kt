package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.asSingle
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber

class GetUploadedPhotosUseCase(
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val apiClient: ApiClient
) {

    private val TAG = "GetUploadedPhotosUseCase"

    fun getUploadedPhotos(lastId: Long, count: Int): Single<Either<ErrorCode, List<UploadedPhoto>>> {
        return async {
            try {
                val userId = settingsRepository.getUserId()
                if (userId.isEmpty()) {
                    throw IllegalStateException("userId cannot be empty!")
                }

                Timber.tag(TAG).d("sending getUploadedPhotos request...")

                val response = apiClient.getUploadedPhotoIds(userId, lastId, count).await()
                val errorCode = response.errorCode

                if (errorCode !is ErrorCode.GetUploadedPhotoIdsError.Remote.Ok) {
                    return@async Either.Error(errorCode)
                }

                val photosResultList = mutableListOf<UploadedPhoto>()
                val uploadedPhotoIds = response.uploadedPhotoIds
                if (uploadedPhotoIds.isEmpty()) {
                    return@async Either.Value(photosResultList)
                }

                val uploadedPhotosFromDb = uploadedPhotosRepository.findMany(uploadedPhotoIds)
                val photoIdsToGetFromServer = Utils.filterListAlreadyContaning(uploadedPhotoIds, uploadedPhotosFromDb.map { it.photoId })

                Timber.tag(TAG).d("Fresh photos' ids = $uploadedPhotoIds")
                Timber.tag(TAG).d("Cached gallery photo ids = ${uploadedPhotosFromDb.map { it.photoId }}")

                if (photoIdsToGetFromServer.isNotEmpty()) {
                    val result = getFreshPhotosFromServer(userId, photoIdsToGetFromServer)
                    if (result is Either.Error) {
                        Timber.tag(TAG).w("Could not get fresh photos from the server, errorCode = ${result.error}")
                        return@async Either.Error(result.error)
                    }

                    (result as Either.Value)

                    Timber.tag(TAG).d("Fresh gallery photo ids = ${result.value.map { it.photoId }}")
                    photosResultList += result.value
                }

                photosResultList.sortByDescending { it.photoId }
                return@async Either.Value(photosResultList)

            } catch (error: Throwable) {
                return@async Either.Error(ErrorCode.GetUploadedPhotoIdsError.Remote.UnknownError())
            }
        }.asSingle(CommonPool)
    }

    private suspend fun getFreshPhotosFromServer(userId: String, photoIds: List<Long>): Either<ErrorCode, List<UploadedPhoto>> {
        val photoIdsToBeRequested = photoIds.joinToString(Constants.PHOTOS_DELIMITER)

        val response = apiClient.getUploadedPhotos(userId, photoIdsToBeRequested).await()
        val errorCode = response.errorCode

        if (errorCode !is ErrorCode.GetGalleryPhotosErrors.Remote.Ok) {
            return Either.Error(errorCode)
        }

        if (!uploadedPhotosRepository.saveMany(response.uploadedPhotos)) {
            return Either.Error(ErrorCode.GetGalleryPhotosErrors.Local.DatabaseError())
        }

        return Either.Value(UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhotos(response.uploadedPhotos))
    }
}