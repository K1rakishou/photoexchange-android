package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosInfoMapper
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import timber.log.Timber

open class GetGalleryPhotosInfoUseCase(
    private val apiClient: ApiClient,
    private val galleryPhotoRepository: GalleryPhotoRepository
) {
    private val TAG = "GetGalleryPhotosInfoUseCase"

    fun loadGalleryPhotosInfo(
        userId: String,
        photos: List<GalleryPhoto>
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, MutableList<GalleryPhoto>>> {
        if (userId.isEmpty()) {
            //return nothing if userId is empty
            return Observable.just(Either.Value(photos as MutableList<GalleryPhoto>))
        }

        val galleryPhotos = photos.toMutableList()

        //we have to set galleryPhotoInfo to empty instead of null for every photo
        //for it to have "favourite" and "report" buttons
        galleryPhotos.forEach { it.galleryPhotoInfo = GalleryPhotoInfo.empty() }

        //if the user has received userId - get photos' additional info
        //(like whether the user has the photo favourited or reported already)
        val galleryPhotoIds = galleryPhotos.map { it.galleryPhotoId }

        //get photos' info by the ids from the database
        galleryPhotoRepository.deleteOldPhotosInfo()
        return Observable.fromCallable { galleryPhotoRepository.findManyInfo(galleryPhotoIds) }
            .concatMap { galleryPhotoInfoFromDb ->
                val photoInfoIdsToGetFromServer = Utils.filterListAlreadyContaining(
                    galleryPhotoIds,
                    galleryPhotoInfoFromDb.map { it.galleryPhotoId }
                )

                updateGalleryPhotoInfo(galleryPhotos, galleryPhotoInfoFromDb)
                Timber.tag(TAG).d("Cached gallery photo info ids = ${galleryPhotoInfoFromDb.map { it.galleryPhotoId }}")

                return@concatMap Observable.just(photoInfoIdsToGetFromServer)
                    .concatMap { photoInfoIds ->
                        getFreshPhotoInfosAndConcatWithCached(userId, galleryPhotos, photoInfoIds, galleryPhotoInfoFromDb)
                    }
            }
            .map { result ->
                if (result !is Either.Value) {
                    return@map result
                }

                result.value.sortByDescending { it.galleryPhotoId }
                return@map result
            }
    }

    private fun getFreshPhotoInfosAndConcatWithCached(
        userId: String,
        galleryPhotos: MutableList<GalleryPhoto>,
        photoInfoIds: List<Long>,
        galleryPhotoInfoFromDb: List<GalleryPhotoInfo>
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, MutableList<GalleryPhoto>>> {
        return getFreshPhotoInfosFromServer(userId, photoInfoIds)
            .concatMap { result ->
                if (result is Either.Value) {
                    updateGalleryPhotoInfo(galleryPhotos, result.value)

                    return@concatMap Observables.zip(
                        Observable.just(galleryPhotoInfoFromDb),
                        Observable.just(result.value),
                        this::combinePhotos
                    )
                }

                return@concatMap Observable.just(Either.Error((result as Either.Error).error))
            }
            .map { Either.Value(galleryPhotos) }
    }

    private fun combinePhotos(
        fromDatabase: List<GalleryPhotoInfo>,
        fromServer: List<GalleryPhotoInfo>
    ): Either.Value<MutableList<GalleryPhotoInfo>> {
        val list = mutableListOf<GalleryPhotoInfo>()
        list += fromDatabase
        list += fromServer

        return Either.Value(list)
    }

    private fun updateGalleryPhotoInfo(
        photosResultList: MutableList<GalleryPhoto>,
        galleryPhotoInfoList: List<GalleryPhotoInfo>
    ) {
        if (photosResultList.isEmpty() || galleryPhotoInfoList.isEmpty()) {
            return
        }

        photosResultList.forEach { galleryPhoto ->
            val galleryPhotoInfo = galleryPhotoInfoList.firstOrNull { galleryPhotoInfo ->
                galleryPhotoInfo.galleryPhotoId == galleryPhoto.galleryPhotoId
            }

            if (galleryPhotoInfo != null) {
                galleryPhoto.galleryPhotoInfo = galleryPhotoInfo
            }
        }
    }

    private fun getFreshPhotoInfosFromServer(
        userId: String,
        photoIds: List<Long>
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhotoInfo>>> {
        return Observable.fromCallable { photoIds.joinToString(Constants.PHOTOS_DELIMITER) }
            .concatMapSingle { photoIdsToBeRequested -> apiClient.getGalleryPhotoInfo(userId, photoIdsToBeRequested) }
            .map { response ->
                val errorCode = response.errorCode as ErrorCode.GetGalleryPhotosErrors
                if (errorCode !is ErrorCode.GetGalleryPhotosErrors.Ok) {
                    return@map Either.Error(errorCode)
                }

                if (!galleryPhotoRepository.saveManyInfo(response.galleryPhotosInfo)) {
                    return@map Either.Error(ErrorCode.GetGalleryPhotosErrors.LocalDatabaseError())
                }

                return@map Either.Value(GalleryPhotosInfoMapper.FromResponse.ToObject.toGalleryPhotoInfoList(response.galleryPhotosInfo))
            }
    }
}