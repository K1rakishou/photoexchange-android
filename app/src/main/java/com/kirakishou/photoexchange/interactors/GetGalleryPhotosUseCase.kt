package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotoInfoMapper
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotoMapper
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.helper.extension.minutes
import com.kirakishou.photoexchange.helper.extension.seconds
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
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
    private val INTERVAL_TO_REFRESH_PHOTO_FROM_SERVER = 30.minutes()

    fun loadPageOfPhotos(userId: String, lastId: Long, photosPerPage: Int): Single<Either<ErrorCode, List<GalleryPhoto>>> {
        return async {
            try {
                Timber.tag(TAG).d("sending request...")

                //get fresh photo ids from the server
                val getGalleryPhotoIdsResponse = apiClient.getGalleryPhotoIds(lastId, photosPerPage).await()
                val getGalleryPhotoIdsErrorCode = getGalleryPhotoIdsResponse.errorCode

                if (getGalleryPhotoIdsErrorCode !is ErrorCode.GetGalleryPhotosErrors.Remote.Ok) {
                    return@async Either.Error(getGalleryPhotoIdsErrorCode)
                }

                val photosResultList = mutableListOf<GalleryPhoto>()
                val galleryPhotoIds = getGalleryPhotoIdsResponse.galleryPhotoIds
                if (galleryPhotoIds.isEmpty()) {
                    return@async Either.Value(photosResultList)
                }

                //get photos by the ids from the database
                val galleryPhotosFromDb = galleryPhotoRepository.findMany(galleryPhotoIds)
                val photoIdsToGetFromServer = filterNotCachedIds(galleryPhotoIds, galleryPhotosFromDb.map { it.galleryPhotoId })
                photosResultList.addAll(galleryPhotosFromDb)

                Timber.tag(TAG).d("Fresh photos' ids = $galleryPhotoIds")
                Timber.tag(TAG).d("Cached gallery photo ids = ${galleryPhotosFromDb.map { it.galleryPhotoId }}")

                //if we got photo ids that are not cached in the DB yet - get the fresh photos
                //by these ids from the server and cache them in the DB
                if (photoIdsToGetFromServer.isNotEmpty()) {
                    val result = getFreshPhotosFromServer(photoIdsToGetFromServer)
                    if (result is Either.Error) {
                        Timber.tag(TAG).w("Could not get fresh photos from the server, errorCode = ${result.error}")
                        return@async Either.Error(result.error)
                    }

                    (result as Either.Value)

                    Timber.tag(TAG).d("Fresh gallery photo ids = ${result.value.map { it.galleryPhotoId }}")
                    photosResultList += result.value
                }

                //if the user has received userId - get photos' additional info
                //(like whether the user has the photo favourited or reported already)
                if (userId.isNotEmpty()) {
                    photosResultList.forEach { it.galleryPhotoInfo = GalleryPhotoInfo.empty() }

                    //get photos' info by the ids from the database
                    val galleryPhotoInfoFromDb = galleryPhotoRepository.findManyInfo(galleryPhotoIds, INTERVAL_TO_REFRESH_PHOTO_FROM_SERVER)
                    val photoInfoIdsToGetFromServer = filterNotCachedIds(galleryPhotoIds, galleryPhotoInfoFromDb.map { it.galleryPhotoId })
                    updateGalleryPhotoInfo(photosResultList, galleryPhotoInfoFromDb)

                    Timber.tag(TAG).d("Cached gallery photo info ids = ${galleryPhotoInfoFromDb.map { it.galleryPhotoId }}")

                    //get the rest from the server
                    if (photoInfoIdsToGetFromServer.isNotEmpty()) {
                        val result = getFreshPhotoInfosFromServer(userId, photoInfoIdsToGetFromServer)
                        if (result is Either.Error) {
                            Timber.tag(TAG).w("Could not get fresh photo info from the server, errorCode = ${result.error}")
                            return@async Either.Error(result.error)
                        }

                        (result as Either.Value)

                        val galleryPhotoInfoList = result.value
                        Timber.tag(TAG).d("Fresh gallery photo info list ids = ${galleryPhotoInfoList.map { it.galleryPhotoId }}")

                        updateGalleryPhotoInfo(photosResultList, galleryPhotoInfoList)
                    }
                }

                photosResultList.sortByDescending { it.galleryPhotoId }
                return@async Either.Value(photosResultList)
            } catch (error: Throwable) {
                Timber.tag(TAG).e(error)
                return@async Either.Error(ErrorCode.GetGalleryPhotosErrors.Remote.UnknownError())
            }
        }.asSingle(CommonPool)
    }

    private fun updateGalleryPhotoInfo(photosResultList: MutableList<GalleryPhoto>, galleryPhotoInfoList: List<GalleryPhotoInfo>) {
        photosResultList.forEach { galleryPhoto ->
            val galleryPhotoInfo = galleryPhotoInfoList.firstOrNull { galleryPhotoInfo ->
                galleryPhotoInfo.galleryPhotoId == galleryPhoto.galleryPhotoId
            }

            if (galleryPhotoInfo == null) {
                return@forEach
            }

            galleryPhoto.galleryPhotoInfo = galleryPhotoInfo
        }
    }

    private suspend fun getFreshPhotoInfosFromServer(userId: String, photoIds: List<Long>): Either<ErrorCode, List<GalleryPhotoInfo>> {
        val photoIdsToBeRequested = photoIds.joinToString(",")

        val response = apiClient.getGalleryPhotoInfo(userId, photoIdsToBeRequested).await()
        val errorCode = response.errorCode

        if (errorCode !is ErrorCode.GetGalleryPhotosInfoError.Remote.Ok) {
            return Either.Error(errorCode)
        }

        if (!galleryPhotoRepository.saveManyInfo(response.galleryPhotosInfo)) {
            return Either.Error(ErrorCode.GetGalleryPhotosInfoError.Local.DatabaseError())
        }

        return Either.Value(GalleryPhotoInfoMapper.FromResponse.ToObject.toGalleryPhotoInfoList(response.galleryPhotosInfo))
    }

    private suspend fun getFreshPhotosFromServer(photoIds: List<Long>): Either<ErrorCode, List<GalleryPhoto>> {
        val photoIdsToBeRequested = photoIds.joinToString(",")

        val response = apiClient.getGalleryPhotos(photoIdsToBeRequested).await()
        val errorCode = response.errorCode

        if (errorCode !is ErrorCode.GetGalleryPhotosErrors.Remote.Ok) {
            return Either.Error(errorCode)
        }

        if (!galleryPhotoRepository.saveMany(response.galleryPhotos)) {
            return Either.Error(ErrorCode.GetGalleryPhotosErrors.Local.DatabaseError())
        }

        return Either.Value(GalleryPhotoMapper.FromResponse.ToObject.toGalleryPhotoList(response.galleryPhotos))
    }

    private fun filterNotCachedIds(freshIdsList: List<Long>, idsFromDbList: List<Long>): List<Long> {
        val freshIdsSet = freshIdsList.toSet()
        val idsFromDbSet = idsFromDbList.toSet()
        val resultList = mutableListOf<Long>()

        if (freshIdsSet.size == idsFromDbSet.size) {
            return resultList
        }

        for (photoId in freshIdsSet) {
            if (!idsFromDbSet.contains(photoId)) {
                resultList += photoId
            }
        }

        return resultList
    }
}