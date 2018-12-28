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

  /**
   * An utility method for paged photo fetching from cache/remote server. Considers network
   * availability and do not access network when current network is metered and it's disabled in
   * the settings to access network with metered connection. Tries to get photos from the cache first
   * and only if there are not enough photos in the cache - tries to access the network. Manages
   * cache cleaning up and fresh photos caching.
   *
   * @param tag - used for debugging logs
   * @param firstUploadedOn - the time when the first photo from the current state was uploaded (MvRx state)
   * @param lastUploadedOn - the time when the last photo from the current state was uploaded (MvRx state)
   * @param requestedCount - the amount of photos we want to be returned
   * @param userId - the id of the current user
   * @param getFreshPhotosCountFunc - a function that fetches the amount of fresh photos on the server
   * @param getPhotosFromCacheFunc - a function that fetches a page of photos from the cache
   * @param getPhotosFromCacheFunc - a function that clears the current photos cache completely
   * @param deleteOldFunc - a function that deletes old cached photos
   * @param mapperFunc - a mapper function that maps photo response to photo object
   * @param filterBannedPhotosFunc - a function that checks whether any fresh photos has been filtered by the user
   * @param cachePhotosFunc - a function that caches fresh photos
   * @return a page of photos
   * */
  //TODO: make getPageOfPhotosFunc return Photo instead of PhotoResponse. After that remove mapperFunc
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
    mapperFunc: suspend (List<PhotoResponse>) -> List<Photo>,
    filterBannedPhotosFunc: suspend (List<Photo>) -> List<Photo>,
    cachePhotosFunc: suspend (List<Photo>) -> Boolean
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
      clearCacheFunc,
      mapperFunc
    )

    val freshPhotosFromServer = when (returnedPhotos) {
      is ReturnedPhotos.FromCache<Photo> -> return returnedPhotos.page
      is ReturnedPhotos.FromServer<Photo> -> returnedPhotos.photos
    }

    if (freshPhotosFromServer.isEmpty()) {
      Timber.tag("${TAG}_$tag").d("No gallery photos were found on the server")
      return Paged(emptyList(), true)
    }

    val filteredPhotos = filterBannedPhotosFunc(freshPhotosFromServer)

    //TODO: should move this inside the when statement because we don't need to cache photos from the cache
    if (!cachePhotosFunc(filteredPhotos)) {
      throw DatabaseException("Could not cache gallery photos in the database")
    }

    //It is important to use "freshPhotosFromServer.size" because it indicates the real amount of the received photos
    //We use that info to figure out whether it is the last page of photos on the server. That's why it is so important.
    return Paged(filteredPhotos, freshPhotosFromServer.size < requestedCount)
  }

  private suspend fun <Photo, PhotoResponse> getPhotos(
    tag: String,
    lastUploadedOn: Long,
    requestedCount: Int,
    userId: String?,
    getFreshPhotosCountResult: FreshPhotosCountRequestResult,
    getPhotosFromCacheFunc: suspend (Long, Int) -> List<Photo>,
    getPageOfPhotosFunc: suspend (String?, Long, Int) -> List<PhotoResponse>,
    clearCacheFunc: suspend () -> Unit,
    mapperFunc: suspend (List<PhotoResponse>) -> List<Photo>
  ): ReturnedPhotos<Photo> {
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
              false
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

          return ReturnedPhotos.FromServer(mapperFunc(photos))
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

        return ReturnedPhotos.FromServer(mapperFunc(photos))
      }
      FreshPhotosCountRequestResult.TooFreshManyPhotos -> {
        Timber.tag("${TAG}_$tag").d("result == TooFreshManyPhotos, count > $requestedCount")

        //if there are more fresh photos than we have requested - invalidate database cache
        //and start loading photos from the first page
        clearCacheFunc()
        val photos = getPageOfPhotosFunc(userId, lastUploadedOn, requestedCount).also {
          Timber.tag("${TAG}_$tag").d("getPageOfPhotosFunc returned ${it.size} photos")
        }

        return ReturnedPhotos.FromServer(mapperFunc(photos))
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

  private sealed class ReturnedPhotos<T> {
    class FromCache<T>(val page: Paged<T>) : ReturnedPhotos<T>()
    class FromServer<T>(val photos: List<T>) : ReturnedPhotos<T>()
  }

  private sealed class FreshPhotosCountRequestResult {
    object NoFreshPhotos : FreshPhotosCountRequestResult()
    object NoInternet : FreshPhotosCountRequestResult()
    object TooFreshManyPhotos : FreshPhotosCountRequestResult()
    class Ok(val count: Int) : FreshPhotosCountRequestResult()
  }

}