package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotoMapper
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.helper.extension.minutes
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.asSingle
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber

class GetGalleryPhotosUseCase(
    private val apiClient: ApiClient,
    private val galleryPhotoRepository: GalleryPhotoRepository
) {
    private val TAG = "GetGalleryPhotosUseCase"

    //interval to update photos in the db with fresh information
    private val INTERVAL_TO_REFRESH_PHOTO_FROM_SERVER = 20.minutes()

    fun loadPageOfPhotos(userId: String, lastId: Long, photosPerPage: Int): Single<Either<ErrorCode, List<GalleryPhoto>>> {
        return async {
            try {
                Timber.tag(TAG).d("sending request...")

                //get fresh photo ids from the server
                val getGalleryPhotoIdsResponse = apiClient.getGalleryPhotoIds(lastId, photosPerPage).await()
                val getGalleryPhotoIdsErrorCode = getGalleryPhotoIdsResponse.errorCode

                if (getGalleryPhotoIdsErrorCode !is ErrorCode.GalleryPhotosErrors.Remote.Ok) {
                    return@async Either.Error(getGalleryPhotoIdsErrorCode)
                }

                val resultList = mutableListOf<GalleryPhoto>()

                //get photos by the ids
                val galleryPhotosFromDb = galleryPhotoRepository.findMany(getGalleryPhotoIdsResponse.galleryPhotoIds)
                val notCachedIds = filterNotCached(getGalleryPhotoIdsResponse.galleryPhotoIds.toSet(), galleryPhotosFromDb.map { it.galleryPhotoId }.toSet())

                resultList.addAll(galleryPhotosFromDb)
                Timber.tag(TAG).d("Fresh photos' ids = ${getGalleryPhotoIdsResponse.galleryPhotoIds}")
                Timber.tag(TAG).d("Cached gallery photo ids = ${galleryPhotosFromDb.map { it.galleryPhotoId }}")

                //if we got photo ids that are not cached in the DB yet - get the fresh photos
                //by these ids from the server and cache them in the DB
                if (notCachedIds.isNotEmpty()) {
                    val result = getFreshPhotosFromServer(notCachedIds)
                    if (result is Either.Error) {
                        Timber.tag(TAG).w("Could not get fresh photos from the server, errorCode = ${result.error}")
                        return@async result
                    }

                    (result as Either.Value)

                    Timber.tag(TAG).d("Fresh gallery photo ids = ${result.value.map { it.galleryPhotoId }}")
                    resultList += result.value
                }

                //if the user has received userId - get photos' additional info
                //(like whether the user has the photo favourited or reported already)
                if (userId.isNotEmpty()) {

                }

                resultList.sortByDescending { it.galleryPhotoId }
                return@async Either.Value(resultList)
            } catch (error: Throwable) {
                Timber.tag(TAG).e(error)
                return@async Either.Error(ErrorCode.GalleryPhotosErrors.Remote.UnknownError())
            }
        }.asSingle(CommonPool)
    }

    private suspend fun getFreshPhotosFromServer(photoIds: List<Long>): Either<ErrorCode, List<GalleryPhoto>> {
        val photoIdsToBeRequestedFromServer = photoIds.joinToString(",")

        val getGalleryPhotosResponse = apiClient.getGalleryPhotos(photoIdsToBeRequestedFromServer).await()
        val getGalleryPhotosErrorCode = getGalleryPhotosResponse.errorCode

        if (getGalleryPhotosErrorCode !is ErrorCode.GalleryPhotosErrors.Remote.Ok) {
            return Either.Error(getGalleryPhotosErrorCode)
        }

        if (!galleryPhotoRepository.saveMany(getGalleryPhotosResponse.galleryPhotos)) {
            return Either.Error(ErrorCode.GalleryPhotosErrors.Local.DatabaseError())
        }

        return Either.Value(GalleryPhotoMapper.toGalleryPhotoList(getGalleryPhotosResponse.galleryPhotos))
    }

    private fun filterNotCached(photoIdsFromServer: Set<Long>, photoIdsFromDb: Set<Long>): List<Long> {
        val resultList = mutableListOf<Long>()

        if (photoIdsFromServer.size == photoIdsFromDb.size) {
            return resultList
        }

        for (photoId in photoIdsFromServer) {
            if (!photoIdsFromDb.contains(photoId)) {
                resultList += photoId
            }
        }

        return resultList
    }
}