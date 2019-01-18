package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.Paged

interface PagedApiUtils {

  suspend fun <T> getPageOfPhotos(
    tag: String,
    firstUploadedOn: Long?,
    lastUploadedOn: Long?,
    requestedCount: Int,
    userUuid: String?,
    getPhotosFromCacheFunc: suspend (Long?, Int) -> List<T>,
    getFreshPhotosFunc: suspend (Long) -> List<T>,
    getPageOfPhotosFunc: suspend (String?, Long?, Int) -> List<T>,
    clearCacheFunc: suspend () -> Unit,
    deleteOldFunc: suspend () -> Unit,
    filterBannedPhotosFunc: suspend (List<T>) -> List<T>,
    cachePhotosFunc: suspend (List<T>) -> Boolean
  ): Paged<T>

}