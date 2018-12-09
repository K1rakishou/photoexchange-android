package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoInfoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoLocalSource
import com.kirakishou.photoexchange.helper.util.TimeUtils

class GalleryPhotosRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource,
  private val galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {

  fun deleteByPhotoName(photoName: String) {
    galleryPhotoLocalSource.deleteByPhotoName(photoName)
    galleryPhotoInfoLocalSource.deleteByPhotoName(photoName)
  }
}