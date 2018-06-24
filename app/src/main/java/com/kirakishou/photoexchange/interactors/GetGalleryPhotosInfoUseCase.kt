package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosInfoMapper
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

class GetGalleryPhotosInfoUseCase(
    private val apiClient: ApiClient,
    private val galleryPhotoRepository: GalleryPhotoRepository
) {
    private val TAG = "GetGalleryPhotosInfoUseCase"

    //interval to update photos in the db with fresh information
    private val INTERVAL_TO_REFRESH_PHOTOS_FROM_SERVER = 30.minutes()

    fun loadGalleryPhotosInfo(userId: String, photos: List<GalleryPhoto>): Single<Either<ErrorCode.GetGalleryPhotosErrors, MutableList<GalleryPhoto>>> {
        return rxSingle {
            val galleryPhotos = photos.toMutableList()
            if (userId.isEmpty()) {
                //return nothing if userId is empty
                return@rxSingle Either.Value(photos as MutableList<GalleryPhoto>)
            }

            //we have to set galleryPhotoInfo to empty instead of null for every photo
            //for it to have "favourite" and "report" buttons
            galleryPhotos.forEach { it.galleryPhotoInfo = GalleryPhotoInfo.empty() }

            //if the user has received userId - get photos' additional info
            //(like whether the user has the photo favourited or reported already)
            val galleryPhotoIds = galleryPhotos.map { it.galleryPhotoId }

            //get photos' info by the ids from the database
            val galleryPhotoInfoFromDb = galleryPhotoRepository.findManyInfo(galleryPhotoIds, INTERVAL_TO_REFRESH_PHOTOS_FROM_SERVER)
            val photoInfoIdsToGetFromServer = Utils.filterListAlreadyContaining(galleryPhotoIds, galleryPhotoInfoFromDb.map { it.galleryPhotoId })
            updateGalleryPhotoInfo(galleryPhotos, galleryPhotoInfoFromDb)

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
                updateGalleryPhotoInfo(galleryPhotos, galleryPhotoInfoList)
            }

            return@rxSingle Either.Value(galleryPhotos)
        }
    }

    private fun updateGalleryPhotoInfo(photosResultList: MutableList<GalleryPhoto>, galleryPhotoInfoList: List<GalleryPhotoInfo>) {
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

    private suspend fun getFreshPhotoInfosFromServer(userId: String, photoIds: List<Long>): Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhotoInfo>> {
        val photoIdsToBeRequested = photoIds.joinToString(Constants.PHOTOS_DELIMITER)

        val response = apiClient.getGalleryPhotoInfo(userId, photoIdsToBeRequested).await()
        val errorCode = response.errorCode as ErrorCode.GetGalleryPhotosErrors

        if (errorCode !is ErrorCode.GetGalleryPhotosErrors.Ok) {
            return Either.Error(errorCode)
        }

        if (!galleryPhotoRepository.saveManyInfo(response.galleryPhotosInfo)) {
            return Either.Error(ErrorCode.GetGalleryPhotosErrors.LocalDatabaseError())
        }

        return Either.Value(GalleryPhotosInfoMapper.FromResponse.ToObject.toGalleryPhotoInfoList(response.galleryPhotosInfo))
    }
}