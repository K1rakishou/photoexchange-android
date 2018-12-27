package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.Paged

interface PagedApiUtils {

  suspend fun <T, R> getPageOfPhotos(
    tag: String,
    firstUploadedOn: Long,
    lastUploadedOn: Long,
    requestedCount: Int,
    userId: String?,
    getFreshPhotosCountFunc: suspend (Long) -> Int,
    getPhotosFromCacheFunc: suspend (Long, Int) -> List<T>,
    getPageOfPhotosFunc: suspend (String?, Long, Int) -> List<R>,
    clearCacheFunc: suspend () -> Unit,
    deleteOldFunc: suspend () -> Unit,
    filterBannedPhotosFunc: suspend (List<R>) -> List<R>,
    cachePhotosFunc: suspend (List<R>) -> Boolean,
    mapperFunc: suspend (List<R>) -> List<T>
  ): Paged<T>

}