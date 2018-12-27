package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import timber.log.Timber

class PagedApiUtilsImpl(
  private val timeUtils: TimeUtils,
  private val netUtils: NetUtils
) : PagedApiUtils {
  private val TAG = "PagedApiUtils"

  override suspend fun <Photo, PhotoResponse> getPageOfPhotos(
    tag: String,  //for debugging
    firstUploadedOn: Long,
    lastUploadedOn: Long,
    requestedCount: Int,
    userId: String?,
    getFreshPhotosCountFunc: suspend (Long) -> Int,
    getPhotosFromCacheFunc: suspend (Long, Int) -> List<Photo>,
    getPageOfPhotosFunc: suspend (String?, Long, Int) -> List<PhotoResponse>,
    clearCacheFunc: suspend () -> Unit,
    deleteOldFunc: suspend () -> Unit,
    filterBannedPhotosFunc: suspend (List<PhotoResponse>) -> List<PhotoResponse>,
    cachePhotosFunc: suspend (List<PhotoResponse>) -> Boolean,
    mapperFunc: suspend (List<PhotoResponse>) -> List<Photo>
  ): Paged<Photo> {
    val getFreshPhotosCountResult = getFreshPhotosCount(
      firstUploadedOn,
      requestedCount,
      tag,
      getFreshPhotosCountFunc
    )

    if (getFreshPhotosCountResult is FreshPhotosCountRequestResult.NoFreshPhotos ||
      getFreshPhotosCountResult is FreshPhotosCountRequestResult.Ok) {
      //delete old photos only when we have internet and when we won't be clearing all cache
      deleteOldFunc()
    }

    val returnedPhotos = getPhotos(
      tag,
      lastUploadedOn,
      requestedCount,
      userId,
      getFreshPhotosCountResult,
      getPhotosFromCacheFunc,
      getPageOfPhotosFunc,
      clearCacheFunc
    )

    val photos = when (returnedPhotos) {
      is ReturnedPhotos.FromCache<*> -> return returnedPhotos.page as Paged<Photo>
      is ReturnedPhotos.FromServer<*> -> returnedPhotos.photos as List<PhotoResponse>
    }

    if (photos.isEmpty()) {
      Timber.tag("${TAG}_$tag").d("No gallery photos were found on the server")
      return Paged(emptyList(), true)
    }

    val filteredPhotos = filterBannedPhotosFunc(photos)

    //TODO: should move this inside the when statement because we don't need to cache photos from the cache
    if (!cachePhotosFunc(filteredPhotos)) {
      throw DatabaseException("Could not cache gallery photos in the database")
    }

    val mappedPhotos = mapperFunc(filteredPhotos)

    //use the "photos.size" not the "filteredPhotos.size"
    return Paged(mappedPhotos, photos.size < requestedCount)
  }

  private suspend fun <Photo, PhotoResponse> getPhotos(
    tag: String,
    lastUploadedOn: Long,
    requestedCount: Int,
    userId: String?,
    getFreshPhotosCountResult: FreshPhotosCountRequestResult,
    getPhotosFromCacheFunc: suspend (Long, Int) -> List<Photo>,
    getPageOfPhotosFunc: suspend (String?, Long, Int) -> List<PhotoResponse>,
    clearCacheFunc: suspend () -> Unit
  ): ReturnedPhotos {
    when (getFreshPhotosCountResult) {
      FreshPhotosCountRequestResult.NoInternet -> {
        Timber.tag("${TAG}_$tag").d("result == NoInternet, just fetch photos from the database")

        val photosFromCache = getPhotosFromCacheFunc(lastUploadedOn, requestedCount)
        return ReturnedPhotos.FromCache(
          Paged(
            photosFromCache,
            photosFromCache.size < requestedCount
          )
        )
      }
      FreshPhotosCountRequestResult.NoFreshPhotos -> {
        Timber.tag("${TAG}_$tag").d("result == NoFreshPhotos")

        //if there are no fresh photos then we can check the cache
        val photosFromCache = getPhotosFromCacheFunc(lastUploadedOn, requestedCount)
        if (photosFromCache.size == requestedCount) {
          Timber.tag("${TAG}_$tag").d("getPhotosFromCacheFunc returned enough photos")

          //if enough photos were found in the cache - return them
          return ReturnedPhotos.FromCache(
            Paged(
              photosFromCache,
              photosFromCache.size < requestedCount
            )
          )
        }

        //if server is not available and this is a first run then getFreshPhotosCount will return NoFreshPhotos
        //if it's not the first run then it will return NoInternet which we don't need to handle here

        try {
          //if there are no fresh photos and not enough photos were found in the cache -
          //get fresh page from the server
          val photos = getPageOfPhotosFunc(userId, lastUploadedOn, requestedCount).also {
            Timber.tag("${TAG}_$tag").d("getPageOfPhotosFunc returned ${it.size} photos")
          }

          return ReturnedPhotos.FromServer(photos)
        } catch (error: ConnectionError) {
          //if the server is still dead then just return whatever there is in the cache
          return ReturnedPhotos.FromCache(
            Paged(
              photosFromCache,
              photosFromCache.size < requestedCount
            )
          )
        }
      }
      is FreshPhotosCountRequestResult.Ok -> {
        Timber.tag("${TAG}_$tag").d("result == ok, count in 1..$requestedCount")

        //otherwise get fresh photos AND the next page and then combine them
        val photos = mutableListOf<PhotoResponse>()

        photos += getPageOfPhotosFunc(
          userId,
          timeUtils.getTimeFast(),
          getFreshPhotosCountResult.count
        ).also {
          Timber.tag("${TAG}_$tag").d("getPageOfPhotosFunc with current time returned ${it.size} photos")
        }
        photos += getPageOfPhotosFunc(userId, lastUploadedOn, requestedCount).also {
          Timber.tag("${TAG}_$tag").d("getPageOfPhotosFunc with the time of last photo returned ${it.size} photos")
        }

        return ReturnedPhotos.FromServer(photos)
      }
      FreshPhotosCountRequestResult.TooFreshManyPhotos -> {
        Timber.tag("${TAG}_$tag").d("result == TooFreshManyPhotos, count > $requestedCount")

        //if there are more fresh photos than we have requested - invalidate database cache
        //and start loading photos from the first page
        clearCacheFunc()
        val photos = getPageOfPhotosFunc(userId, lastUploadedOn, requestedCount).also {
          Timber.tag("${TAG}_$tag").d("getPageOfPhotosFunc returned ${it.size} photos")
        }

        return ReturnedPhotos.FromServer(photos)
      }
    }
  }

  private suspend fun getFreshPhotosCount(
    firstUploadedOn: Long,
    requestedCount: Int,
    tag: String,
    getFreshPhotosCountFunc: suspend (Long) -> Int
  ): FreshPhotosCountRequestResult {
    if (!netUtils.canAccessNetwork()) {
      return FreshPhotosCountRequestResult.NoInternet
    }

    try {
      //firstUploadedOn == -1L means that we have not fetched any photos from the server yet so
      //there is no point in checking whether there are fresh photos
      if (firstUploadedOn == -1L) {
        Timber.tag("${TAG}_$tag").d("firstUploadedOn == -1, first call of this method")
        return FreshPhotosCountRequestResult.NoFreshPhotos
      } else {
        val count = getFreshPhotosCountFunc(firstUploadedOn).also {
          Timber.tag("${TAG}_$tag").d("getFreshPhotosCountFunc called and returned $it")
        }

        return when {
          count == 0 -> FreshPhotosCountRequestResult.NoFreshPhotos
          count > requestedCount -> FreshPhotosCountRequestResult.TooFreshManyPhotos
          else -> FreshPhotosCountRequestResult.Ok(count)
        }
      }
    } catch (error: ConnectionError) {
      Timber.tag("${TAG}_$tag").e(error)

      //do not attempt to get photos from the server when there is no internet connection
      return FreshPhotosCountRequestResult.NoInternet
    }
  }

  private sealed class ReturnedPhotos {
    class FromCache<Photos>(val page: Paged<Photos>) : ReturnedPhotos()
    class FromServer<PhotoResponse>(val photos: List<PhotoResponse>) : ReturnedPhotos()
  }

  private sealed class FreshPhotosCountRequestResult {
    object NoFreshPhotos : FreshPhotosCountRequestResult()
    object NoInternet : FreshPhotosCountRequestResult()
    object TooFreshManyPhotos : FreshPhotosCountRequestResult()
    class Ok(val count: Int) : FreshPhotosCountRequestResult()
  }

}