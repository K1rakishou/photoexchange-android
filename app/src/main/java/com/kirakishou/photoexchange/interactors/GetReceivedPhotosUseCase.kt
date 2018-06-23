package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClientImpl
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.GetReceivedPhotosException
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import timber.log.Timber

class GetReceivedPhotosUseCase(
    private val database: MyDatabase,
    private val receivedPhotosRepository: ReceivedPhotosRepository,
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val apiClient: ApiClientImpl
) {

    private val TAG = "GetReceivedPhotosUseCase"

    fun loadPageOfPhotos(
        userId: String,
        lastId: Long,
        count: Int
    ): Observable<Either<ErrorCode.GetReceivedPhotosErrors, MutableList<ReceivedPhoto>>> {
        Timber.tag(TAG).d("sending loadPageOfPhotos request...")

        return apiClient.getReceivedPhotoIds(userId, lastId, count).toObservable()
            .concatMap { response ->
                val errorCode = response.errorCode as ErrorCode.GetReceivedPhotosErrors
                if (errorCode !is ErrorCode.GetReceivedPhotosErrors.Ok) {
                    throw GetReceivedPhotosException.OnKnownError(errorCode)
                }

                return@concatMap Observable.just(response.receivedPhotoIds)
                    .concatMap { receivedPhotoIds ->
                        val receivedPhotosFromDb = receivedPhotosRepository.findMany(receivedPhotoIds)
                        val photoIdsToGetFromServer = Utils.filterListAlreadyContaining(receivedPhotoIds, receivedPhotosFromDb.map { it.photoId })

                        return@concatMap Observable.just(photoIdsToGetFromServer)
                            .concatMap { photoIds ->
                                if (photoIds.isNotEmpty()) {
                                    return@concatMap getFreshPhotosAndConcatWithCached(userId, photoIds, receivedPhotosFromDb)
                                }

                                return@concatMap Observable.just(Either.Value(receivedPhotosFromDb))
                            }
                    }
            }
            .onErrorReturn { error ->
                Timber.tag(TAG).e(error)
                return@onErrorReturn handleErrors(error)
            }
    }

    private fun handleErrors(error: Throwable): Either<ErrorCode.GetReceivedPhotosErrors, MutableList<ReceivedPhoto>> {
        if (error is GetReceivedPhotosException) {
            return when (error) {
                is GetReceivedPhotosException.OnKnownError -> Either.Error(error.errorCode)
            }
        }

        return Either.Error(ErrorCode.GetReceivedPhotosErrors.UnknownError())
    }

    private fun getFreshPhotosAndConcatWithCached(
        userId: String, photoIds: List<Long>,
        receivedPhotosFromDb: List<ReceivedPhoto>
    ): Observable<Either<ErrorCode.GetReceivedPhotosErrors, MutableList<ReceivedPhoto>>> {
        return getFreshPhotosFromServer(userId, photoIds)
            .concatMap { result ->
                if (result is Either.Value) {
                    return@concatMap Observables.zip(Observable.just(receivedPhotosFromDb), Observable.just(result.value), { fromDatabase, fromServer ->
                        val list = mutableListOf<ReceivedPhoto>()
                        list += fromDatabase
                        list += fromServer

                        return@zip Either.Value(list)
                    })
                }

                return@concatMap Observable.just(Either.Error((result as Either.Error).error))
            }
    }

    private fun getFreshPhotosFromServer(userId: String, photoIds: List<Long>): Observable<Either<ErrorCode.GetReceivedPhotosErrors, MutableList<ReceivedPhoto>>> {
        return Observable.fromCallable { photoIds.joinToString(Constants.PHOTOS_DELIMITER) }
            .concatMapSingle { photoIdsToBeRequested -> apiClient.getReceivedPhotos(userId, photoIdsToBeRequested) }
            .map { response ->
                val errorCode = response.errorCode as ErrorCode.GetReceivedPhotosErrors
                if (errorCode !is ErrorCode.GetReceivedPhotosErrors.Ok) {
                    return@map Either.Error(errorCode)
                }

                if (response.receivedPhotos.isEmpty()) {
                    return@map Either.Value(mutableListOf<ReceivedPhoto>())
                }

                val transactionResult = database.transactional {
                    for (receivedPhoto in response.receivedPhotos) {
                        uploadedPhotosRepository.updateReceiverInfo(receivedPhoto.uploadedPhotoName)
                    }

                    if (!receivedPhotosRepository.saveMany(response.receivedPhotos)) {
                        return@transactional false
                    }

                    return@transactional true
                }

                if (!transactionResult) {
                    return@map Either.Error(ErrorCode.GetReceivedPhotosErrors.DatabaseError())
                }

                return@map Either.Value(ReceivedPhotosMapper.FromResponse.GetReceivedPhotos.toReceivedPhotos(response.receivedPhotos))
            }
    }
}