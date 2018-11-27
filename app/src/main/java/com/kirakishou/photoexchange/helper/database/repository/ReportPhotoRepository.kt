package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoInfoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoLocalSource
import com.kirakishou.photoexchange.helper.database.source.remote.ReportPhotoRemoteSource
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import kotlinx.coroutines.withContext
import java.lang.Exception

open class ReportPhotoRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val reportPhotoRemoteSource: ReportPhotoRemoteSource,
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource,
  private val galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {

  open suspend fun reportPhoto(userId: String, photoName: String): Boolean {
    return withContext(coroutineContext) {
      val isReported = reportPhotoRemoteSource.reportPhoto(userId, photoName)

      try {
        reportInDatabase(photoName, isReported)
      } catch (error: Exception) {
        throw DatabaseException(error.message)
      }

      return@withContext isReported
    }
  }

  private suspend fun reportInDatabase(photoName: String, isReported: Boolean) {
    database.transactional {
      val galleryPhotoEntity = galleryPhotoLocalSource.findByPhotoName(photoName)
      if (galleryPhotoEntity == null) {
        //TODO: should an exception be thrown here?
        return@transactional
      }

      var galleryPhotoInfoEntity = galleryPhotoInfoLocalSource.find(galleryPhotoEntity.photoName)
      if (galleryPhotoInfoEntity == null) {
        galleryPhotoInfoEntity = GalleryPhotoInfoEntity.create(
          galleryPhotoEntity.photoName,
          false,
          0,
          isReported,
          timeUtils.getTimeFast()
        )
      } else {
        galleryPhotoInfoEntity.isReported = isReported
      }

      if (!galleryPhotoInfoLocalSource.save(galleryPhotoInfoEntity)) {
        throw DatabaseException("Could not update gallery photo info")
      }
    }
  }

}