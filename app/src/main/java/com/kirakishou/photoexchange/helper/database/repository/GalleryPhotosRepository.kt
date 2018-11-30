package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.util.TimeUtils
import kotlinx.coroutines.withContext

class GalleryPhotosRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val galleryPhotoDao = database.galleryPhotoDao()
  private val galleryPhotoInfoDao = database.galleryPhotoInfoDao()

  suspend fun deleteAll() {
    withContext(coroutineContext) {
      database.transactional {
        galleryPhotoDao.deleteAll()
        galleryPhotoInfoDao.deleteAll()
      }
    }
  }

  suspend fun deleteOldPhotos() {
    withContext(coroutineContext) {
      database.transactional {
        val now = timeUtils.getTimeFast()

        galleryPhotoDao.deleteOlderThan(now - Constants.GALLERY_PHOTOS_CACHE_MAX_LIVE_TIME)
        galleryPhotoInfoDao.deleteOlderThan(now - Constants.GALLERY_PHOTOS_INFO_CACHE_MAX_LIVE_TIME)
      }
    }
  }
}