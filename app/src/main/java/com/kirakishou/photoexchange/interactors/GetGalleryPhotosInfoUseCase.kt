package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosInfoMapper
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.exception.EmptyUserIdException
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.Exception

open class GetGalleryPhotosInfoUseCase(
  private val apiClient: ApiClient,
  private val galleryPhotoRepository: GalleryPhotoRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetGalleryPhotosInfoUseCase"

  suspend fun loadGalleryPhotosInfo(
    userId: String,
    photos: List<GalleryPhoto>
  ): Either<Exception, List<GalleryPhoto>> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        if (userId.isEmpty()) {
          //return nothing if userId is empty
          return@myRunCatching photos
        }

        //delete old photos info (if there are any)
        galleryPhotoRepository.deleteOldPhotosInfo()
        val galleryPhotos = photos.toMutableList()

        //we have to set galleryPhotoInfo to empty instead of null for every photo
        //for it to have "favourite" and "report" buttons
        galleryPhotos.forEach { it.galleryPhotoInfo = GalleryPhotoInfo.empty() }

        //if the user has received userId - get photos' additional info
        //(like whether the user has the photo favourited or reported already)
        val galleryPhotoIds = galleryPhotos.map { it.galleryPhotoId }
        val galleryPhotoInfoFromDb = galleryPhotoRepository.findManyInfo(galleryPhotoIds)
        Timber.tag(TAG).d("Cached gallery photo info ids = ${galleryPhotoInfoFromDb.map { it.galleryPhotoId }}")

        if (galleryPhotoInfoFromDb.size == photos.size) {
          return@myRunCatching updateGalleryPhotoInfo(photos, galleryPhotoInfoFromDb)
        }

        if (userId.isEmpty()) {
          throw EmptyUserIdException()
        }

        val freshPhotoInfo = getFreshPhotoInfosFromServer(userId, galleryPhotoIds)
        return@myRunCatching updateGalleryPhotoInfo(galleryPhotos, freshPhotoInfo)
          .sortedByDescending { it.galleryPhotoId }
      }
    }
  }

  private fun updateGalleryPhotoInfo(
    photosResultList: List<GalleryPhoto>,
    galleryPhotoInfoList: List<GalleryPhotoInfo>
  ): List<GalleryPhoto> {
    return photosResultList.map { galleryPhoto ->
      val galleryPhotoInfo = galleryPhotoInfoList.firstOrNull { galleryPhotoInfo ->
        galleryPhotoInfo.galleryPhotoId == galleryPhoto.galleryPhotoId
      }

      return@map galleryPhoto.copy(galleryPhotoInfo = galleryPhotoInfo)
    }
  }

  private suspend fun getFreshPhotoInfosFromServer(
    userId: String,
    photoIds: List<Long>
  ): List<GalleryPhotoInfo> {
    val idsString = photoIds.joinToString(Constants.PHOTOS_DELIMITER)
    val response = apiClient.getGalleryPhotoInfo(userId, idsString).await()

    val errorCode = response.errorCode as ErrorCode.GetGalleryPhotosErrors
    if (errorCode !is ErrorCode.GetGalleryPhotosErrors.Ok) {
      throw ApiException(errorCode)
    }

    if (!galleryPhotoRepository.saveManyInfo(response.galleryPhotosInfo)) {
      throw ApiException(ErrorCode.GetGalleryPhotosErrors.LocalDatabaseError())
    }

    return GalleryPhotosInfoMapper.FromResponse.ToObject.toGalleryPhotoInfoList(response.galleryPhotosInfo)
  }
}