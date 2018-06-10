package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import timber.log.Timber

class GetUploadedPhotosUseCase(
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val apiClient: ApiClient
) {

    private val TAG = "GetUploadedPhotosUseCase"

    fun loadPageOfPhotos(userId: String, lastId: Long, count: Int): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        Timber.tag(TAG).d("sending loadPageOfPhotos request...")

        return apiClient.getUploadedPhotoIds(userId, lastId, count).toObservable()
            .concatMap { response ->
                val errorCode = response.errorCode as ErrorCode.GetUploadedPhotosErrors

                if (errorCode !is ErrorCode.GetUploadedPhotosErrors.Ok) {
                    return@concatMap Observable.just(Either.Error(errorCode))
                }

                val photosResultList = mutableListOf<UploadedPhoto>()
                val uploadedPhotoIds = response.uploadedPhotoIds
                if (uploadedPhotoIds.isEmpty()) {
                    return@concatMap Observable.just(Either.Value(photosResultList))
                }

                val uploadedPhotosFromDb = uploadedPhotosRepository.findMany(uploadedPhotoIds)
                val photoIdsToGetFromServer = Utils.filterListAlreadyContaining(uploadedPhotoIds, uploadedPhotosFromDb.map { it.photoId })
                photosResultList += uploadedPhotosFromDb

                return@concatMap Observable.just(photoIdsToGetFromServer)
                    .concatMap { photoIds ->
                        if (photoIds.isNotEmpty()) {
                            return@concatMap getFreshPhotosAndConcatWithCached(userId, photoIds, uploadedPhotosFromDb)
                        }

                        return@concatMap Observable.just(Either.Value(uploadedPhotosFromDb))
                    }
            }
    }

    private fun getFreshPhotosAndConcatWithCached(
        userId: String,
        photoIds: List<Long>,
        uploadedPhotosFromDb: List<UploadedPhoto>
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, MutableList<UploadedPhoto>>> {
        return getFreshPhotosFromServer(userId, photoIds)
            .concatMap { result ->
                if (result is Either.Value) {
                    return@concatMap Observables.zip(Observable.just(uploadedPhotosFromDb), Observable.just(result.value), { fromDatabase, fromServer ->
                        val list = mutableListOf<UploadedPhoto>()
                        list += fromDatabase
                        list += fromServer

                        return@zip Either.Value(list)
                    })
                }

                return@concatMap Observable.just(Either.Error((result as Either.Error).error))
            }
    }

    private fun getFreshPhotosFromServer(
        userId: String,
        photoIds: List<Long>
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        return Observable.fromCallable { photoIds.joinToString(Constants.PHOTOS_DELIMITER) }
            .concatMapSingle { photoIdsToBeRequested -> apiClient.getUploadedPhotos(userId, photoIdsToBeRequested) }
            .map { response ->
                val errorCode = response.errorCode as ErrorCode.GetUploadedPhotosErrors

                if (errorCode !is ErrorCode.GetUploadedPhotosErrors.Ok) {
                    return@map Either.Error(errorCode)
                }

                if (response.uploadedPhotos.isEmpty()) {
                    return@map Either.Value(listOf<UploadedPhoto>())
                }

                if (!uploadedPhotosRepository.saveMany(response.uploadedPhotos)) {
                    return@map Either.Error(ErrorCode.GetUploadedPhotosErrors.DatabaseError())
                }

                return@map Either.Value(UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhotos(response.uploadedPhotos))
            }
    }
}