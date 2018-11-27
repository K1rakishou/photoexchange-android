package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.api.response.FavouritePhotoResponseData
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoInfoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoLocalSource
import com.kirakishou.photoexchange.helper.database.source.remote.FavouritePhotoRemoteSource
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.mvp.model.FavouritePhotoActionResult
import kotlinx.coroutines.withContext
import java.lang.Exception

open class FavouritePhotoRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val favouritePhotoRemoteSource: FavouritePhotoRemoteSource,
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource,
  private val galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {

  open suspend fun favouritePhoto(userId: String, photoName: String): FavouritePhotoActionResult {
    return withContext(coroutineContext) {
      val favouritePhotoResult = favouritePhotoRemoteSource.favouritePhoto(userId, photoName)

      try {
        favouriteInDatabase(photoName, favouritePhotoResult)
      } catch (error: Throwable) {
        throw DatabaseException(error.message)
      }

      return@withContext FavouritePhotoActionResult(
        photoName,
        favouritePhotoResult.isFavourited,
        favouritePhotoResult.favouritesCount
      )
    }
  }

  private suspend fun favouriteInDatabase(photoName: String, favouritePhotoResponseData: FavouritePhotoResponseData) {
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
          favouritePhotoResponseData.isFavourited,
          favouritePhotoResponseData.favouritesCount,
          false,
          timeUtils.getTimeFast()
        )
      } else {
        galleryPhotoInfoEntity.isFavourited = favouritePhotoResponseData.isFavourited
      }

      if (!galleryPhotoInfoLocalSource.save(galleryPhotoInfoEntity)) {
        throw DatabaseException("Could not update gallery photo info ")
      }
    }
  }
}