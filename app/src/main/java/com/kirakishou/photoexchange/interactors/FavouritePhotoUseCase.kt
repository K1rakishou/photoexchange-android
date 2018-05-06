package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import timber.log.Timber

class FavouritePhotoUseCase(
    private val apiClient: ApiClient,
    private val galleryPhotoRepository: GalleryPhotoRepository
) {
    private val TAG = "FavouritePhotoUseCase"

    fun favouritePhoto(userId: String, photoName: String): Observable<Either<ErrorCode, FavouritePhotoResult>> {
        return apiClient.favouritePhoto(userId, photoName)
            .map { response ->
                val errorCode = response.errorCode
                if (errorCode !is ErrorCode.FavouritePhotoErrors.Remote.Ok) {
                    return@map Either.Error(errorCode)
                }

                return@map Either.Value(FavouritePhotoResult(response.isFavourited, response.favouritesCount))
            }
            .toObservable()
            .doOnError { Timber.tag(TAG).e(it) }
    }

    data class FavouritePhotoResult(val isFavourited: Boolean,
                                    val favouritesCount: Long)
}