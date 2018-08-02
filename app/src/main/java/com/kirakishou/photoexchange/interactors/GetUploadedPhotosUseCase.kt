package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.GetUploadedPhotosException
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotoIdsResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import timber.log.Timber

open class GetUploadedPhotosUseCase(
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val apiClient: ApiClient
) {

    private val TAG = "GetUploadedPhotosUseCase"

    open fun loadPageOfPhotos(userId: String, lastId: Long, count: Int): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        Timber.tag(TAG).d("sending loadPageOfPhotos request...")

        return apiClient.getUploadedPhotoIds(userId, lastId, count).toObservable()
            .concatMap { response -> handleResponse(response, userId) }
            .onErrorReturn { error ->
                Timber.tag(TAG).e(error)
                return@onErrorReturn handleErrors(error)
            }
            .map { result ->
                if (result !is Either.Value) {
                    return@map result
                }

                return@map Either.Value(result.value.sortedByDescending { it.photoId })
            }
    }

    private fun handleResponse(
        response: GetUploadedPhotoIdsResponse, userId: String
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        val errorCode = response.errorCode as ErrorCode.GetUploadedPhotosErrors

        if (errorCode !is ErrorCode.GetUploadedPhotosErrors.Ok) {
            throw GetUploadedPhotosException.OnKnownError(errorCode)
        }

        val photosResultList = mutableListOf<UploadedPhoto>()
        val uploadedPhotoIds = response.uploadedPhotoIds
        if (uploadedPhotoIds.isEmpty()) {
            return Observable.just(Either.Value(photosResultList))
        }

        uploadedPhotosRepository.deleteOld()
        val uploadedPhotosFromDb = uploadedPhotosRepository.findMany(uploadedPhotoIds)
        val photoIdsToGetFromServer = Utils.filterListAlreadyContaining(uploadedPhotoIds, uploadedPhotosFromDb.map { it.photoId })
        photosResultList += uploadedPhotosFromDb

        return Observable.just(photoIdsToGetFromServer)
            .concatMap { photoIds -> getFreshPhotosAndConcatWithCached(userId, photoIds, uploadedPhotosFromDb) }
    }

    private fun handleErrors(error: Throwable): Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>> {
        if (error is GetUploadedPhotosException) {
            return when (error) {
                is GetUploadedPhotosException.OnKnownError -> Either.Error(error.errorCode)
            }
        }

        return Either.Error(ErrorCode.GetUploadedPhotosErrors.UnknownError())
    }

    private fun getFreshPhotosAndConcatWithCached(
        userId: String,
        photoIds: List<Long>,
        uploadedPhotosFromDb: List<UploadedPhoto>
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        if (photoIds.isNotEmpty()) {
            return getFreshPhotosFromServer(userId, photoIds)
                .concatMap { result -> combinePhotos(result, uploadedPhotosFromDb) }
        }

        return Observable.just(Either.Value(uploadedPhotosFromDb))

    }

    private fun combinePhotos(
        result: Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>,
        uploadedPhotosFromDb: List<UploadedPhoto>
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        if (result is Either.Value) {
            return Observables.zip(
                Observable.just(uploadedPhotosFromDb),
                Observable.just(result.value),
                this::combineFunction
            )
        }

        //if we could not get fresh photos from server - return what we could find in the database
        return Observable.just(Either.Value(uploadedPhotosFromDb))
    }

    private fun combineFunction(
        fromDatabase: List<UploadedPhoto>,
        fromServer: List<UploadedPhoto>
    ): Either.Value<MutableList<UploadedPhoto>> {
        val list = mutableListOf<UploadedPhoto>()
        list += fromDatabase
        list += fromServer

        return Either.Value(list)
    }

    private fun getFreshPhotosFromServer(
        userId: String,
        photoIds: List<Long>
    ): Observable<Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>>> {
        return Observable.fromCallable { photoIds.joinToString(Constants.PHOTOS_DELIMITER) }
            .concatMapSingle { photoIdsToBeRequested -> apiClient.getUploadedPhotos(userId, photoIdsToBeRequested) }
            .map(this::cacheFreshPhotos)
    }

    private fun cacheFreshPhotos(
        response: GetUploadedPhotosResponse
    ): Either<ErrorCode.GetUploadedPhotosErrors, List<UploadedPhoto>> {
        val errorCode = response.errorCode as ErrorCode.GetUploadedPhotosErrors

        if (errorCode !is ErrorCode.GetUploadedPhotosErrors.Ok) {
            return Either.Error(errorCode)
        }

        if (response.uploadedPhotos.isEmpty()) {
            return Either.Value(listOf<UploadedPhoto>())
        }

        if (!uploadedPhotosRepository.saveMany(response.uploadedPhotos)) {
            return Either.Error(ErrorCode.GetUploadedPhotosErrors.DatabaseError())
        }

        return Either.Value(UploadedPhotosMapper.FromResponse.ToObject.toUploadedPhotos(response.uploadedPhotos))
    }
}