package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.exception.GetGalleryPhotosException
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GetGalleryPhotosUseCase(
  private val apiClient: ApiClient,
  private val galleryPhotoRepository: GalleryPhotoRepository,
  private val timeUtils: TimeUtils
) : BaseUseCase() {
  private val TAG = "GetGalleryPhotosUseCase"

  //ErrorCode.GetGalleryPhotosErrors
  open suspend fun loadPageOfPhotos(
    lastUploadedOn: Long,
    count: Int
  ): Either<Exception, List<GalleryPhoto>> {
    Timber.tag(TAG).d("sending loadPageOfPhotos request...")

    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val time = if (lastUploadedOn != -1L) {
          lastUploadedOn
        } else {
          timeUtils.getTimeFast()
        }

        //if we found exactly the same amount of gallery photos that was requested - return them
        val pageOfGalleryPhotos = galleryPhotoRepository.getPageOfGalleryPhotos(time, count)
        if (pageOfGalleryPhotos.size == count) {
          return@myRunCatching pageOfGalleryPhotos
        }

        //otherwise reload the page from the server
        val response = apiClient.getPageOfGalleryPhotos(lastUploadedOn, count).await()
        val errorCode = response.errorCode as ErrorCode.GetGalleryPhotosErrors

        if (errorCode !is ErrorCode.GetGalleryPhotosErrors.Ok) {
          throw GetGalleryPhotosException.OnKnownError(errorCode)
        }

        if (response.galleryPhotos.isEmpty()) {
          return@myRunCatching emptyList<GalleryPhoto>()
        }

        if (!galleryPhotoRepository.saveMany(response.galleryPhotos)) {
          throw GetGalleryPhotosException.OnKnownError(ErrorCode.GetGalleryPhotosErrors.LocalDatabaseError())
        }

        return@myRunCatching GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(response.galleryPhotos)
      }
    }
  }
}