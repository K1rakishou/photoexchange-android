package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoInfoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoLocalSource
import com.kirakishou.photoexchange.helper.util.TimeUtils
import kotlinx.coroutines.withContext

class ReportPhotoRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource,
  private val galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {

  open suspend fun reportPhoto(photoName: String, isReported: Boolean) {
    withContext(coroutineContext) {
      database.transactional {
        val galleryPhotoEntity = galleryPhotoLocalSource.findByPhotoName(photoName)
        if (galleryPhotoEntity == null) {
          return@transactional
        }

        var galleryPhotoInfoEntity = galleryPhotoInfoLocalSource.findById(galleryPhotoEntity.galleryPhotoId)
        if (galleryPhotoInfoEntity == null) {
          galleryPhotoInfoEntity = GalleryPhotoInfoEntity.create(
            galleryPhotoEntity.galleryPhotoId,
            false,
            0,
            isReported,
            timeUtils.getTimeFast()
          )
        } else {
          galleryPhotoInfoEntity.isReported = isReported
        }

        galleryPhotoInfoLocalSource.save(galleryPhotoInfoEntity)
      }
    }
  }

}