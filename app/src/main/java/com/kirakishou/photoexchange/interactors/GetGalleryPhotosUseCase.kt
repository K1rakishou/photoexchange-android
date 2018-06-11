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

    fun loadPageOfPhotos(lastId: Long, photosPerPage: Int): Single<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        return rxSingle {
            try {
                Timber.tag(TAG).d("sending loadPageOfPhotos request...")

                //get fresh photo ids from the server
                val getGalleryPhotoIdsResponse = apiClient.getGalleryPhotoIds(lastId, photosPerPage).await()
                val getGalleryPhotoIdsErrorCode = getGalleryPhotoIdsResponse.errorCode as ErrorCode.GetGalleryPhotosErrors

                if (getGalleryPhotoIdsErrorCode !is ErrorCode.GetGalleryPhotosErrors.Ok) {
                    return@rxSingle Either.Error(getGalleryPhotoIdsErrorCode)
                }

                val photosResultList = mutableListOf<GalleryPhoto>()
                val galleryPhotoIds = getGalleryPhotoIdsResponse.galleryPhotoIds
                if (galleryPhotoIds.isEmpty()) {
                    return@rxSingle Either.Value(photosResultList)
                }

                //get photos by the ids from the database
                val galleryPhotosFromDb = galleryPhotoRepository.findMany(galleryPhotoIds)
                val photoIdsToGetFromServer = Utils.filterListAlreadyContaining(galleryPhotoIds, galleryPhotosFromDb.map { it.galleryPhotoId })
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
                    photosResultList += result.value
                }

                photosResultList.sortByDescending { it.galleryPhotoId }
                return@rxSingle Either.Value(photosResultList)
            } catch (error: Throwable) {
                Timber.tag(TAG).e(error)
                return@rxSingle Either.Error(ErrorCode.GetGalleryPhotosErrors.UnknownError())
            }
        }
    }

    private suspend fun getFreshPhotosFromServer(photoIds: List<Long>): Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>> {
        val photoIdsToBeRequested = photoIds.joinToString(Constants.PHOTOS_DELIMITER)

        val response = apiClient.getGalleryPhotos(photoIdsToBeRequested).await()
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