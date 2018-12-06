package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import timber.log.Timber

class PagedApiUtilsImpl(
  val timeUtils: TimeUtils
) : PagedApiUtils {
  private val TAG = "PagedApiUtils"

  override suspend fun <T, R> getPageOfPhotos(
    tag: String,
    firstUploadedOn: Long,
    lastUploadedOn: Long,
    count: Int,
    userId: String?,
    getFreshPhotosCountFunc: suspend (Long) -> Int,
    getPhotosFromCacheFunc: suspend (Long, Int) -> Paged<T>,
    getPageOfPhotosFunc: suspend (String?, Long, Int) -> List<R>,
    clearCacheFunc: suspend () -> Unit,
    cachePhotosFunc: suspend (List<R>) -> Boolean,
    mapperFunc: suspend (List<R>) -> List<T>
  ): Paged<T> {
    val freshPhotosCount = try {
      //firstUploadedOn == -1L means that we have not fetched any photos from the server yet so
      //there is no point in checking whether there are fresh photos
      if (firstUploadedOn == -1L) {
        Timber.tag("${TAG}_$tag").d("firstUploadedOn == -1, first call of this method")
        0
      } else {
        getFreshPhotosCountFunc(firstUploadedOn).also {
          Timber.tag("${TAG}_$tag").d("getFreshPhotosCountFunc called and returned $it")
        }
      }
    } catch (error: ConnectionError) {
      Timber.tag("${TAG}_$tag").e(error)

      //do not attempt to get photos from the server when there is no internet connection
      -1
    }

    val photos = when (freshPhotosCount) {
      -1 -> {
        Timber.tag("${TAG}_$tag").d("freshPhotosCount == -1, just fetch photos from the database")
        return getPhotosFromCacheFunc(lastUploadedOn, count)
      }
      in 0..count -> {
        Timber.tag("${TAG}_$tag").d("freshPhotosCount in 0..$count")

        if (freshPhotosCount == 0) {
          Timber.tag("${TAG}_$tag").d("freshPhotosCount == 0")

          //if there are no fresh photos then we can check the cache
          val fromCache = getPhotosFromCacheFunc(lastUploadedOn, count)
          if (fromCache.page.size == count) {
            Timber.tag("${TAG}_$tag").d("getPhotosFromCacheFunc returned enough photos")

            //if enough photos were found in the cache - return them
            return fromCache
          }

          //if there are no fresh photos and not enough photos were found in the cache -
          //get fresh page from the server
          getPageOfPhotosFunc(userId, lastUploadedOn, count).also {
            Timber.tag("${TAG}_$tag").d("getPageOfPhotosFunc returned ${it.size} photos")
          }
        } else {
          Timber.tag("${TAG}_$tag").d("freshPhotosCount > 0")

          //otherwise get fresh photos AND the next page and then combine them
          val photos = mutableListOf<R>()

          photos += getPageOfPhotosFunc(userId, timeUtils.getTimeFast(), freshPhotosCount).also {
            Timber.tag("${TAG}_$tag").d("getPageOfPhotosFunc with current time returned ${it.size} photos")
          }
          photos += getPageOfPhotosFunc(userId, lastUploadedOn, count).also {
            Timber.tag("${TAG}_$tag").d("getPageOfPhotosFunc with the time of last photo returned ${it.size} photos")
          }

          photos
        }
      }
      else -> {
        Timber.tag("${TAG}_$tag").d("freshPhotosCount > $count")

        //if there are more fresh photos than we have requested - invalidate database cache
        //and start loading photos from the first page
        clearCacheFunc()
        getPageOfPhotosFunc(userId, lastUploadedOn, count).also {
          Timber.tag("${TAG}_$tag").d("getPageOfPhotosFunc returned ${it.size} photos")
        }
      }
    }

    if (photos.isEmpty()) {
      Timber.tag("${TAG}_$tag").d("No gallery photos were found on the server")
      return Paged(emptyList(), true)
    }

    if (!cachePhotosFunc(photos)) {
      throw DatabaseException("Could not cache gallery photos in the database")
    }

    val mappedPhotos = mapperFunc(photos)
    return Paged(mappedPhotos, photos.size < count)
  }

}