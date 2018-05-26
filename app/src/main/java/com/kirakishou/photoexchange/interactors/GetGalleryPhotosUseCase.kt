package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosInfoMapper
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.helper.extension.minutes
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.rx2.rxSingle
import timber.log.Timber

class GetGalleryPhotosUseCase(
    private val apiClient: ApiClient,
    private val galleryPhotoRepository: GalleryPhotoRepository
) {
    private val TAG = "GetGalleryPhotosUseCase"

    //interval to update photos in the db with fresh information
    private val INTERVAL_TO_REFRESH_PHOTOS_FROM_SERVER = 30.minutes()

    fun loadPageOfPhotos(userId: String, lastId: Long, photosPerPage: Int): Single<Either<ErrorCode, List<GalleryPhoto>>> {
        return rxSingle {
            try {
                Timber.tag(TAG).d("sending loadPageOfPhotos request...")

                //get fresh photo ids from the server
                val getGalleryPhotoIdsResponse = apiClient.getGalleryPhotoIds(lastId, photosPerPage).await()
                val getGalleryPhotoIdsErrorCode = getGalleryPhotoIdsResponse.errorCode

                if (getGalleryPhotoIdsErrorCode !is ErrorCode.GalleryPhotosErrors.Ok) {
                    return@rxSingle Either.Error(getGalleryPhotoIdsErrorCode)
                }

                val photosResultList = mutableListOf<GalleryPhoto>()
                val galleryPhotoIds = getGalleryPhotoIdsResponse.galleryPhotoIds
                if (galleryPhotoIds.isEmpty()) {
                    return@rxSingle Either.Value(photosResultList)
                }

                //get photos by the ids from the database
                val galleryPhotosFromDb = galleryPhotoRepository.findMany(galleryPhotoIds)
                val photoIdsToGetFromServer = Utils.filterListAlreadyContaning(galleryPhotoIds, galleryPhotosFromDb.map { it.galleryPhotoId })
                photosResultList.addAll(galleryPhotosFromDb)

                //if we got photo ids that are not cached in the DB yet - get the fresh photos
                //by these ids from the server and cache them in the DB
                if (photoIdsToGetFromServer.isNotEmpty()) {
                    val result = getFreshPhotosFromServer(photoIdsToGetFromServer)
                    if (result is Either.Error) {
                        Timber.tag(TAG).w("Could not get fresh photos from the server, errorCode = ${result.error}")
                        return@rxSingle Either.Error(result.error)
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
                    val galleryPhotoInfoFromDb = galleryPhotoRepository.findManyInfo(galleryPhotoIds, INTERVAL_TO_REFRESH_PHOTOS_FROM_SERVER)
                    val photoInfoIdsToGetFromServer = Utils.filterListAlreadyContaning(galleryPhotoIds, galleryPhotoInfoFromDb.map { it.galleryPhotoId })
                    updateGalleryPhotoInfo(photosResultList, galleryPhotoInfoFromDb)

                    Timber.tag(TAG).d("Cached gallery photo info ids = ${galleryPhotoInfoFromDb.map { it.galleryPhotoId }}")

                    //get the rest from the server
                    if (photoInfoIdsToGetFromServer.isNotEmpty()) {
                        val result = getFreshPhotoInfosFromServer(userId, photoInfoIdsToGetFromServer)
                        if (result is Either.Error) {
                            Timber.tag(TAG).w("Could not get fresh photo info from the server, errorCode = ${result.error}")
                            return@rxSingle Either.Error(result.error)
                        }

                        (result as Either.Value)

                        val galleryPhotoInfoList = result.value
                        Timber.tag(TAG).d("Fresh gallery photo info list ids = ${galleryPhotoInfoList.map { it.galleryPhotoId }}")

                        updateGalleryPhotoInfo(photosResultList, galleryPhotoInfoList)
                    }
                }

                photosResultList.sortByDescending { it.galleryPhotoId }
                return@rxSingle Either.Value(photosResultList)
            } catch (error: Throwable) {
                Timber.tag(TAG).e(error)
                return@rxSingle Either.Error(ErrorCode.GalleryPhotosErrors.UnknownError())
            }
        }
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
        val photoIdsToBeRequested = photoIds.joinToString(Constants.PHOTOS_DELIMITER)

        val response = apiClient.getGalleryPhotoInfo(userId, photoIdsToBeRequested).await()
        val errorCode = response.errorCode

        if (errorCode !is ErrorCode.GalleryPhotosErrors.Ok) {
            return Either.Error(errorCode)
        }

        if (!galleryPhotoRepository.saveManyInfo(response.galleryPhotosInfo)) {
            return Either.Error(ErrorCode.GalleryPhotosErrors.LocalDatabaseError())
        }

        return Either.Value(GalleryPhotosInfoMapper.FromResponse.ToObject.toGalleryPhotoInfoList(response.galleryPhotosInfo))
    }

    private suspend fun getFreshPhotosFromServer(photoIds: List<Long>): Either<ErrorCode, List<GalleryPhoto>> {
        val photoIdsToBeRequested = photoIds.joinToString(Constants.PHOTOS_DELIMITER)

        val response = apiClient.getGalleryPhotos(photoIdsToBeRequested).await()
        val errorCode = response.errorCode

        if (errorCode !is ErrorCode.GalleryPhotosErrors.Ok) {
            return Either.Error(errorCode)
        }

        if (!galleryPhotoRepository.saveMany(response.galleryPhotos)) {
            return Either.Error(ErrorCode.GalleryPhotosErrors.LocalDatabaseError())
        }

        return Either.Value(GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(response.galleryPhotos))
    }
}