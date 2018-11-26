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

  open suspend fun favouritePhoto(userId: String, photoName: String): FavouritePhotoResponseData {
    return withContext(coroutineContext) {
      val favouritePhotoResult = favouritePhotoRemoteSource.favouritePhoto(userId, photoName)

      try {
        favouriteInDatabase(photoName, favouritePhotoResult)
      } catch (error: Throwable) {
        throw DatabaseException(error.message)
      }

      return@withContext favouritePhotoResult
    }
  }

  private suspend fun favouriteInDatabase(photoName: String, favouritePhotoResponseData: FavouritePhotoResponseData) {
    database.transactional {
      val galleryPhotoEntity = galleryPhotoLocalSource.findByPhotoName(photoName)
      if (galleryPhotoEntity == null) {
        //TODO: should an exception be thrown here?
        return@transactional
      }

      var galleryPhotoInfoEntity = galleryPhotoInfoLocalSource.findById(galleryPhotoEntity.galleryPhotoId)
      if (galleryPhotoInfoEntity == null) {
        galleryPhotoInfoEntity = GalleryPhotoInfoEntity.create(
          galleryPhotoEntity.galleryPhotoId,
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