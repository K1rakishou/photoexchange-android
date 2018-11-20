package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosInfoMapper
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoInfoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.SettingsLocalSource
import com.kirakishou.photoexchange.helper.database.source.remote.GalleryPhotoInfoRemoteSource
import com.kirakishou.photoexchange.helper.database.source.remote.GalleryPhotoRemoteSource
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
import com.kirakishou.photoexchange.mvp.model.exception.DatabaseException
import kotlinx.coroutines.withContext
import java.lang.Exception

open class GetGalleryPhotosRepository(
  private val database: MyDatabase,
  private val settingsLocalSource: SettingsLocalSource,
  private val galleryPhotoRemoteSource: GalleryPhotoRemoteSource,
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource,
  private val galleryPhotoInfoRemoteSource: GalleryPhotoInfoRemoteSource,
  private val galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "GetGalleryPhotosRepository"

  suspend fun getPage(time: Long, count: Int): Either<Exception, List<GalleryPhoto>> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        deleteOld()

        val galleryPhotos = getPageOfGalleryPhotos(time, count)
        val galleryPhotoIds = galleryPhotos.map { it.galleryPhotoId }

        val userId = settingsLocalSource.getUserId()
        val galleryPhotosInfo = getGalleryPhotosInfo(userId, galleryPhotoIds)

        for (galleryPhoto in galleryPhotos) {
          galleryPhoto.galleryPhotoInfo = galleryPhotosInfo
            .firstOrNull { it.galleryPhotoId == galleryPhoto.galleryPhotoId }
        }

        return@myRunCatching galleryPhotos
          .sortedByDescending { it.galleryPhotoId }
      }
    }
  }

  private suspend fun deleteOld() {
    database.transactional {
      galleryPhotoLocalSource.deleteOld()
      galleryPhotoInfoLocalSource.deleteOld()
    }
  }

  private suspend fun getPageOfGalleryPhotos(time: Long, count: Int): List<GalleryPhoto> {
    //if we found exactly the same amount of gallery photos that was requested - return them
    val cachedGalleryPhotos = galleryPhotoLocalSource.getPage(time, count)
    if (cachedGalleryPhotos.size == count) {
      return cachedGalleryPhotos
    }

    //otherwise reload the page from the server
    val galleryPhotos = galleryPhotoRemoteSource.getPageOfGalleryPhotos(time, count)
    if (galleryPhotos.isEmpty()) {
      return cachedGalleryPhotos
    }

    if (!galleryPhotoLocalSource.saveMany(galleryPhotos)) {
      throw DatabaseException("Could not cache gallery photos in the database")
    }

    return GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(galleryPhotos)
  }

  private suspend fun getGalleryPhotosInfo(userId: String, galleryPhotoIds: List<Long>): List<GalleryPhotoInfo> {
    val cachedGalleryPhotosInfo = galleryPhotoInfoLocalSource.findMany(galleryPhotoIds)
    if (cachedGalleryPhotosInfo.size == galleryPhotoIds.size) {
      return cachedGalleryPhotosInfo
    }

    val requestString = galleryPhotoIds.joinToString()
    val galleryPhotosInfo = galleryPhotoInfoRemoteSource.getGalleryPhotoInfo(userId, requestString)

    if (!galleryPhotoInfoLocalSource.saveMany(galleryPhotosInfo)) {
      throw DatabaseException("Could not cache gallery photo info in the database")
    }

    return GalleryPhotosInfoMapper.FromResponseData.ToObject.toGalleryPhotoInfoList(galleryPhotosInfo)
  }

}