package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotoMapper
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
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

    fun loadNextPageOfGalleryPhotos(userId: String, lastId: Long, photosPerPage: Int): Single<Either<ErrorCode, List<GalleryPhoto>>> {
        return async {
            Timber.tag(TAG).d("sending getGalleryPhotoIds request...")

            val getGalleryPhotoIdsResponse = apiClient.getGalleryPhotoIds(lastId, photosPerPage).await()
            val getGalleryPhotoIdsErrorCode = getGalleryPhotoIdsResponse.errorCode

            Timber.tag(TAG).d("Got getGalleryPhotoIdsResponse answer, errorCode is $getGalleryPhotoIdsErrorCode")
            if (getGalleryPhotoIdsErrorCode !is ErrorCode.GalleryPhotosErrors.Remote.Ok) {
                return@async Either.Error(getGalleryPhotoIdsErrorCode)
            }

            val resultList = mutableListOf<GalleryPhoto>()
            val galleryPhotosFromDb = galleryPhotoRepository.findMany(getGalleryPhotoIdsResponse.galleryPhotoIds)
            val notCachedIds = getPhotoIdsNotCachedInDb(getGalleryPhotoIdsResponse.galleryPhotoIds.toSet(), galleryPhotosFromDb.map { it.galleryPhotoId }.toSet())

            Timber.tag(TAG).d("galleryPhotoIds1 = ${getGalleryPhotoIdsResponse.galleryPhotoIds}")
            Timber.tag(TAG).d("galleryPhotosFromDb = ${galleryPhotosFromDb.map { it.galleryPhotoId }}")
            Timber.tag(TAG).d("notCachedIds = ${notCachedIds}")

            resultList.addAll(galleryPhotosFromDb)

            if (notCachedIds.isNotEmpty()) {
                val photoIdsToBeRequestedFromServer = notCachedIds.joinToString(",")

                Timber.tag(TAG).d("sending getGalleryPhotos request with ids = $photoIdsToBeRequestedFromServer...")
                val getGalleryPhotosResponse = apiClient.getGalleryPhotos(userId, photoIdsToBeRequestedFromServer).await()
                val getGalleryPhotosErrorCode = getGalleryPhotosResponse.errorCode

                Timber.tag(TAG).d("Got getGalleryPhotosResponse answer, errorCode is $getGalleryPhotosErrorCode")
                if (getGalleryPhotosErrorCode !is ErrorCode.GalleryPhotosErrors.Remote.Ok) {
                    return@async Either.Error(getGalleryPhotosErrorCode)
                }

                Timber.tag(TAG).d("galleryPhotoIds2 = ${getGalleryPhotosResponse.galleryPhotos.map { it.id }}")
                if (!galleryPhotoRepository.saveMany(getGalleryPhotosResponse.galleryPhotos)) {
                    return@async Either.Error(ErrorCode.GalleryPhotosErrors.Local.DatabaseError())
                }

                resultList.addAll(GalleryPhotoMapper.toGalleryPhotoList(getGalleryPhotosResponse.galleryPhotos))
            }

            resultList.sortByDescending { it.galleryPhotoId }

            Timber.tag(TAG).d("resultList = ${resultList.map { it.galleryPhotoId }}")
            return@async Either.Value(resultList)
        }.asSingle(CommonPool)
    }

    private fun getPhotoIdsNotCachedInDb(photoIdsFromServer: Set<Long>, photoIdsFromDb: Set<Long>): List<Long> {
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