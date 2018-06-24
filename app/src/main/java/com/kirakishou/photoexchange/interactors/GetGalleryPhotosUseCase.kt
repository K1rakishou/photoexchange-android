package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.exception.GetGalleryPhotosException
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import timber.log.Timber

class GetGalleryPhotosUseCase(
    private val apiClient: ApiClient,
    private val galleryPhotoRepository: GalleryPhotoRepository
) {
    private val TAG = "GetGalleryPhotosUseCase"

    fun loadPageOfPhotos(
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        Timber.tag(TAG).d("sending loadPageOfPhotos request...")

        return apiClient.getGalleryPhotoIds(lastId, photosPerPage).toObservable()
            .concatMap { response ->
                val errorCode = response.errorCode as ErrorCode.GetGalleryPhotosErrors
                if (errorCode !is ErrorCode.GetGalleryPhotosErrors.Ok) {
                    throw GetGalleryPhotosException.OnKnownError(errorCode)
                }

                return@concatMap Observable.just(response.galleryPhotoIds)
                    .concatMap { galleryPhotoIds ->
                        val galleryPhotosFromDb = galleryPhotoRepository.findMany(galleryPhotoIds)
                        val photoIdsToGetFromServer = Utils.filterListAlreadyContaining(
                            galleryPhotoIds,
                            galleryPhotosFromDb.map { it.galleryPhotoId }
                        )

                        return@concatMap Observable.just(photoIdsToGetFromServer)
                            .concatMap { photoIds ->
                                if (photoIds.isNotEmpty()) {
                                    return@concatMap getFreshPhotosAndConcatWithCached(photoIdsToGetFromServer, galleryPhotosFromDb)
                                }

                                return@concatMap Observable.just(Either.Value(galleryPhotosFromDb))
                            }
                    }
            }
            .onErrorReturn { error ->
                Timber.tag(TAG).e(error)
                return@onErrorReturn handleErrors(error)
            }
    }

    private fun handleErrors(
        error: Throwable
    ): Either<ErrorCode.GetGalleryPhotosErrors, MutableList<GalleryPhoto>> {
        if (error is GetGalleryPhotosException) {
            return when (error) {
                is GetGalleryPhotosException.OnKnownError -> Either.Error(error.errorCode)
            }
        }

        return Either.Error(ErrorCode.GetGalleryPhotosErrors.UnknownError())
    }

    private fun getFreshPhotosAndConcatWithCached(
        photoIdsToGetFromServer: List<Long>,
        galleryPhotosFromDb: List<GalleryPhoto>
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        return getFreshPhotosFromServer(photoIdsToGetFromServer)
            .concatMap { result ->
                if (result is Either.Value) {
                    return@concatMap Observables.zip(
                        Observable.just(galleryPhotosFromDb),
                        Observable.just(result.value),
                        this::combinePhotos
                    )
                }

                return@concatMap Observable.just(Either.Error((result as Either.Error).error))
            }
    }

    private fun combinePhotos(
        fromDatabase: List<GalleryPhoto>,
        fromServer: List<GalleryPhoto>
    ): Either.Value<MutableList<GalleryPhoto>> {
        val list = mutableListOf<GalleryPhoto>()
        list += fromDatabase
        list += fromServer

        return Either.Value(list)
    }

    private fun getFreshPhotosFromServer(
        photoIds: List<Long>
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        return Observable.fromCallable { photoIds.joinToString(Constants.PHOTOS_DELIMITER) }
            .concatMapSingle { photoIdsToBeRequested -> apiClient.getGalleryPhotos(photoIdsToBeRequested) }
            .map { response ->
                val errorCode = response.errorCode as ErrorCode.GetGalleryPhotosErrors

                if (errorCode !is ErrorCode.GetGalleryPhotosErrors.Ok) {
                    return@map Either.Error(errorCode)
                }

                if (!galleryPhotoRepository.saveMany(response.galleryPhotos)) {
                    return@map Either.Error(ErrorCode.GetGalleryPhotosErrors.LocalDatabaseError())
                }

                return@map Either.Value(GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(response.galleryPhotos))
            }
    }
}