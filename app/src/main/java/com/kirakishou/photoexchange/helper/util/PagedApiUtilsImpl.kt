package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.exception.AttemptToAccessInternetWithMeteredNetworkException
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import timber.log.Timber

class PagedApiUtilsImpl(
  private val timeUtils: TimeUtils
) : PagedApiUtils {
  private val TAG = "PagedApiUtils"

  //TODO: make getPageOfPhotosFunc return Photo instead of PhotoResponse. After that remove mapperFunc
  override suspend fun <PhotoType, PhotoResponseType> getPageOfPhotos(
    tag: String,  //for debugging
    firstUploadedOn: Long,
    lastUploadedOn: Long,
    requestedCount: Int,
    userUuid: String?,
    getPhotosFromCacheFunc: suspend (Long, Int) -> List<PhotoType>,
    //Either throws an AttemptToAccessInternetWithMeteredNetworkException or returns list of fresh photos
    getFreshPhotosFunc: suspend (Long) -> List<PhotoType>,
    getPageOfPhotosFunc: suspend (String?, Long, Int) -> List<PhotoResponseType>,
    clearCacheFunc: suspend () -> Unit,
    deleteOldFunc: suspend () -> Unit,
    mapperFunc: suspend (List<PhotoResponseType>) -> List<PhotoType>,
    filterBannedPhotosFunc: suspend (List<PhotoType>) -> List<PhotoType>,
    cachePhotosFunc: suspend (List<PhotoType>) -> Boolean
  ): Paged<PhotoType> {
    val freshPhotos = try {
      getFreshPhotosFunc(firstUploadedOn)
    } catch (error: AttemptToAccessInternetWithMeteredNetworkException) {
      // Error here means that we can't access internet (current network is metered) so
      // the only thing we can do is to return whatever there is in the cache
      val photosFromCache = getPhotosFromCacheFunc(lastUploadedOn, requestedCount)
      return Paged(photosFromCache, photosFromCache.size < requestedCount)
    }

    deleteOldFunc()

    val freshPhotosCountRequestResult = when {
      freshPhotos.isEmpty() -> FreshPhotosCountRequestResult.NoFreshPhotos
      freshPhotos.size > requestedCount -> FreshPhotosCountRequestResult.TooFreshManyPhotos
      else -> FreshPhotosCountRequestResult.Ok(freshPhotos.size)
    }

    val returnedPhotos = getPhotos(
      tag,
      lastUploadedOn,
      requestedCount,
      userUuid,
      freshPhotosCountRequestResult,
      getPhotosFromCacheFunc,
      getPageOfPhotosFunc,
      clearCacheFunc,
      mapperFunc
    )

    val pageOfPhotos = when (returnedPhotos) {
      is ReturnedPhotos.FromCache<PhotoType> -> return returnedPhotos.page
      is ReturnedPhotos.FromServer<PhotoType> -> returnedPhotos.photos
    }

    val combinedPhotos = freshPhotos + pageOfPhotos
    if (combinedPhotos.isEmpty()) {
      Timber.tag("${TAG}_$tag").d("No gallery photos were found on the server")
      return Paged(emptyList(), true)
    }

    val filteredPhotos = filterBannedPhotosFunc(combinedPhotos)
    if (!cachePhotosFunc(filteredPhotos)) {
      throw DatabaseException("Could not cache gallery photos in the database")
    }

    return Paged(filteredPhotos, pageOfPhotos.size < requestedCount)
  }

  private suspend fun <PhotoType, PhotoResponseType> getPhotos(
    tag: String,
    lastUploadedOn: Long,
    requestedCount: Int,
    userId: String?,
    getFreshPhotosCountResult: FreshPhotosCountRequestResult,
    getPhotosFromCacheFunc: suspend (Long, Int) -> List<PhotoType>,
    getPageOfPhotosFunc: suspend (String?, Long, Int) -> List<PhotoResponseType>,
    clearCacheFunc: suspend () -> Unit,
    mapperFunc: suspend (List<PhotoResponseType>) -> List<PhotoType>
  ): ReturnedPhotos<PhotoType> {
    when (getFreshPhotosCountResult) {
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

        //if there are no fresh photos and not enough photos were found in the cache -
        //get fresh page from the server
        val photos = getPageOfPhotosFunc(userId, lastUploadedOn, requestedCount).also {
          Timber.tag("${TAG}_$tag").d("getPageOfPhotosFunc returned ${it.size} photos")
        }

        return ReturnedPhotos.FromServer(mapperFunc(photos))
      }
      is FreshPhotosCountRequestResult.Ok -> {
        Timber.tag("${TAG}_$tag").d("result == ok, count in 1..$requestedCount")

        //otherwise get the next page
        val photos = getPageOfPhotosFunc(userId, lastUploadedOn, requestedCount).also {
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

  private sealed class ReturnedPhotos<T> {
    class FromCache<T>(val page: Paged<T>) : ReturnedPhotos<T>()
    class FromServer<T>(val photos: List<T>) : ReturnedPhotos<T>()
  }

  private sealed class FreshPhotosCountRequestResult {
    object NoFreshPhotos : FreshPhotosCountRequestResult()
    object TooFreshManyPhotos : FreshPhotosCountRequestResult()
    class Ok(val count: Int) : FreshPhotosCountRequestResult()
  }

}