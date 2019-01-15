package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.Paged

interface PagedApiUtils {

  suspend fun <T, R> getPageOfPhotos(
    tag: String,
    firstUploadedOn: Long,
    lastUploadedOn: Long,
    requestedCount: Int,
    userUuid: String?,
    getPhotosFromCacheFunc: suspend (Long, Int) -> List<T>,
    getFreshPhotosFunc: suspend (Long) -> List<T>,
    getPageOfPhotosFunc: suspend (String?, Long, Int) -> List<R>,
    clearCacheFunc: suspend () -> Unit,
    deleteOldFunc: suspend () -> Unit,
    mapperFunc: suspend (List<R>) -> List<T>,
    filterBannedPhotosFunc: suspend (List<T>) -> List<T>,
    cachePhotosFunc: suspend (List<T>) -> Boolean
  ): Paged<T>

}