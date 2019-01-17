package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.exception.AttemptToAccessInternetWithMeteredNetworkException
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import timber.log.Timber

class PagedApiUtilsImpl(
  private val timeUtils: TimeUtils
) : PagedApiUtils {
  private val TAG = "PagedApiUtils"

  override suspend fun <PhotoType> getPageOfPhotos(
    tag: String,  //for debugging
    firstUploadedOn: Long,
    lastUploadedOn: Long,
    requestedCount: Int,
    userUuid: String?,
    getPhotosFromCacheFunc: suspend (Long, Int) -> List<PhotoType>,
    //Either throws an AttemptToAccessInternetWithMeteredNetworkException or returns list of fresh photos
    getFreshPhotosFunc: suspend (Long) -> List<PhotoType>,
    getPageOfPhotosFunc: suspend (String?, Long, Int) -> List<PhotoType>,
    clearCacheFunc: suspend () -> Unit,
    deleteOldFunc: suspend () -> Unit,
    filterBannedPhotosFunc: suspend (List<PhotoType>) -> List<PhotoType>,
    cachePhotosFunc: suspend (List<PhotoType>) -> Boolean
  ): Paged<PhotoType> {
    val freshPhotos = try {
      if (firstUploadedOn == -1L) {
        emptyList()
      } else {
        getFreshPhotosFunc(firstUploadedOn)
      }
    } catch (error: Throwable) {
      when (error) {
        is AttemptToAccessInternetWithMeteredNetworkException,
        is ConnectionError -> {
          // Error here means that we can't access internet (current network is metered) so
          // the only thing we can do is to return whatever there is in the cache.
          // We also don't want to delete old photos at this point.
          val photosFromCache = getPhotosFromCacheFunc(lastUploadedOn, requestedCount)
          return Paged(photosFromCache, photosFromCache.size < requestedCount)
        }
        else -> throw error
      }
    }

    val freshPhotosCountRequestResult = when {
      freshPhotos.isEmpty() -> FreshPhotosCountRequestResult.NoFreshPhotos
      freshPhotos.size > requestedCount -> FreshPhotosCountRequestResult.TooFreshManyPhotos
      else -> FreshPhotosCountRequestResult.Ok(freshPhotos.size)
    }

    when (freshPhotosCountRequestResult) {
      is FreshPhotosCountRequestResult.Ok,
      FreshPhotosCountRequestResult.NoFreshPhotos -> {
        deleteOldFunc()
      }
      FreshPhotosCountRequestResult.TooFreshManyPhotos -> {
        //do nothing
      }
    }

    val returnedPhotos = getPhotos(
      tag,
      lastUploadedOn,
      requestedCount,
      userUuid,
      freshPhotosCountRequestResult,
      getPhotosFromCacheFunc,
      getPageOfPhotosFunc,
      clearCacheFunc
    )

    val pageOfPhotos = when (returnedPhotos) {
      is ReturnedPhotos.FromCache<PhotoType> -> {
        //filter out any banned photos from fresh photos list
        val filteredPhotos = if (freshPhotos.isNotEmpty()) {
          filterBannedPhotosFunc(freshPhotos)
        } else {
          emptyList()
        }

        // If we have enough photos in the cache - concatenate them with fresh photos
        // (if there are any) and return them as a page
        return Paged(filteredPhotos + returnedPhotos.photos, returnedPhotos.photos.size < requestedCount)
      }
      is ReturnedPhotos.FromServer<PhotoType> -> {
        returnedPhotos.photos
      }
    }

    //do not combine fresh photos with pageOfPhotos if there are more fresh photos than requested
    val combinedPhotos = if (freshPhotos.size > requestedCount) {
      pageOfPhotos
    } else {
      freshPhotos + pageOfPhotos
    }

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

  private suspend fun <PhotoType> getPhotos(
    tag: String,
    lastUploadedOn: Long,
    requestedCount: Int,
    userId: String?,
    getFreshPhotosCountResult: FreshPhotosCountRequestResult,
    getPhotosFromCacheFunc: suspend (Long, Int) -> List<PhotoType>,
    getPageOfPhotosFunc: suspend (String?, Long, Int) -> List<PhotoType>,
    clearCacheFunc: suspend () -> Unit
  ): ReturnedPhotos<PhotoType> {
    when (getFreshPhotosCountResult) {
      FreshPhotosCountRequestResult.NoFreshPhotos -> {
        Timber.tag("${TAG}_$tag").d("result == NoFreshPhotos")

        //if there are no fresh photos then we can check the cache
        val photosFromCache = getPhotosFromCacheFunc(lastUploadedOn, requestedCount)
        if (photosFromCache.size == requestedCount) {
          Timber.tag("${TAG}_$tag").d("getPhotosFromCacheFunc returned enough photos")

          //if enough photos were found in the cache - return them
          return ReturnedPhotos.FromCache(photosFromCache)
        }

        //if server is not available and this is a first run then getFreshPhotosCount will return NoFreshPhotos
        //if it's not the first run then it will return NoInternet which we don't need to handle here

        //if there are no fresh photos and not enough photos were found in the cache -
        //get fresh page from the server
        val photos = getPageOfPhotosFunc(userId, lastUploadedOn, requestedCount).also {
          Timber.tag("${TAG}_$tag").d("getPageOfPhotosFunc returned ${it.size} photos")
        }

        return ReturnedPhotos.FromServer(photos)
      }
      is FreshPhotosCountRequestResult.Ok -> {
        Timber.tag("${TAG}_$tag").d("result == ok, count in 1..$requestedCount")

        //otherwise get the next page
        val photos = getPageOfPhotosFunc(userId, lastUploadedOn, requestedCount).also {
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

  private sealed class ReturnedPhotos<T> {
    class FromCache<T>(val photos: List<T>) : ReturnedPhotos<T>()
    class FromServer<T>(val photos: List<T>) : ReturnedPhotos<T>()
  }

  private sealed class FreshPhotosCountRequestResult {
    //no photos were uploaded since the last check
    object NoFreshPhotos : FreshPhotosCountRequestResult()

    // TooFreshManyPhotos means that there were uploaded more than one full page of photo since
    // the last time user has checked. This means, that we can't load them in one request, so it's
    // better to just completely wipe cache and start loading photos from the beginning
    object TooFreshManyPhotos : FreshPhotosCountRequestResult()


    class Ok(val count: Int) : FreshPhotosCountRequestResult()
  }

}