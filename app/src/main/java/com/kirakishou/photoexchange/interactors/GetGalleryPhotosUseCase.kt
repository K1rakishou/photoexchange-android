package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.exception.GetGalleryPhotosException
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotoIdsResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import timber.log.Timber

open class GetGalleryPhotosUseCase(
    private val apiClient: ApiClient,
    private val galleryPhotoRepository: GalleryPhotoRepository
) {
    private val TAG = "GetGalleryPhotosUseCase"

    open fun loadPageOfPhotos(
        lastId: Long,
        photosPerPage: Int
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        Timber.tag(TAG).d("sending loadPageOfPhotos request...")

        return apiClient.getGalleryPhotoIds(lastId, photosPerPage).toObservable()
            .concatMap(this::handleResponse)
            .onErrorReturn { error ->
                Timber.tag(TAG).e(error)
                return@onErrorReturn handleErrors(error)
            }
            .map { result ->
                if (result !is Either.Value) {
                    return@map result
                }

                return@map Either.Value(result.value.sortedByDescending { it.galleryPhotoId })
            }
    }

    private fun handleResponse(
        response: GalleryPhotoIdsResponse
    ): Observable<out Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        val errorCode = response.errorCode as ErrorCode.GetGalleryPhotosErrors
        if (errorCode !is ErrorCode.GetGalleryPhotosErrors.Ok) {
            throw GetGalleryPhotosException.OnKnownError(errorCode)
        }

        if (response.galleryPhotoIds.isEmpty()) {
            return Observable.just(Either.Value(listOf()))
        }

        return Observable.just(response.galleryPhotoIds)
            .concatMap(this::getPhotosFromDbOrFromServer)
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

    private fun getPhotosFromDbOrFromServer(
        galleryPhotoIds: List<Long>
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        galleryPhotoRepository.deleteOldPhotos()
        val galleryPhotosFromDb = galleryPhotoRepository.findMany(galleryPhotoIds)
        val photoIdsToGetFromServer = Utils.filterListAlreadyContaining(
            galleryPhotoIds,
            galleryPhotosFromDb.map { it.galleryPhotoId }
        )

        return Observable.just(photoIdsToGetFromServer)
            .concatMap { photoIds -> combinePhotos(photoIds, galleryPhotosFromDb) }
    }

    private fun combinePhotos(
        photoIdsToGetFromServer: List<Long>,
        galleryPhotosFromDb: List<GalleryPhoto>
    ): Observable<Either.Value<List<GalleryPhoto>>> {
        if (photoIdsToGetFromServer.isNotEmpty()) {
            return getFreshPhotosFromServer(photoIdsToGetFromServer)
                .concatMap { result ->
                    if (result is Either.Value) {
                        return@concatMap Observables.zip(
                            Observable.just(galleryPhotosFromDb),
                            Observable.just(result.value),
                            this::combineFunction
                        )
                    }

                    //if we could not get fresh photos from server - return what we could find in the database
                    return@concatMap Observable.just(Either.Value(galleryPhotosFromDb))
                }
        }

        return Observable.just(Either.Value(galleryPhotosFromDb))
    }

    private fun combineFunction(
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
            .map(this::cacheFreshPhotos)
    }

    private fun cacheFreshPhotos(response: GalleryPhotosResponse): Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>> {
        val errorCode = response.errorCode as ErrorCode.GetGalleryPhotosErrors

        if (errorCode !is ErrorCode.GetGalleryPhotosErrors.Ok) {
            return Either.Error(errorCode)
        }

        if (!galleryPhotoRepository.saveMany(response.galleryPhotos)) {
            return Either.Error(ErrorCode.GetGalleryPhotosErrors.LocalDatabaseError())
        }

        return Either.Value(GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(response.galleryPhotos))
    }
}