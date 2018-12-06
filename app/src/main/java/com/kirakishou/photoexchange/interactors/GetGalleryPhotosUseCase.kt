package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GetGalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import kotlinx.coroutines.withContext
import timber.log.Timber

open class GetGalleryPhotosUseCase(
  private val settingsRepository: SettingsRepository,
  private val getGalleryPhotosRepository: GetGalleryPhotosRepository,
  private val timeUtils: TimeUtils,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetGalleryPhotosUseCase"

  open suspend fun loadPageOfPhotos(
    forced: Boolean,
    firstUploadedOn: Long,
    lastUploadedOnParam: Long,
    count: Int
  ): Paged<GalleryPhoto> {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("loadPageOfPhotos called")

      val (lastUploadedOn, userId) = getParameters(lastUploadedOnParam)
      return@withContext getGalleryPhotosRepository.getPage(
        forced,
        userId,
        firstUploadedOn,
        lastUploadedOn,
        count
      )
    }
  }

  private suspend fun getParameters(lastUploadedOnParam: Long): Pair<Long, String> {
    val lastUploadedOn = if (lastUploadedOnParam != -1L) {
      lastUploadedOnParam
    } else {
      timeUtils.getTimeFast()
    }

    //empty userId is allowed here since we need it only when fetching galleryPhotoInfo
    val userId = settingsRepository.getUserId()
    return lastUploadedOn to userId
  }
}