package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import timber.log.Timber

open class FavouritePhotoUseCase(
    private val apiClient: ApiClient,
    private val database: MyDatabase,
    private val galleryPhotoRepository: GalleryPhotoRepository
) {
    private val TAG = "FavouritePhotoUseCase"

    fun favouritePhoto(userId: String, photoName: String): Observable<Either<ErrorCode.FavouritePhotoErrors, FavouritePhotoResult>> {
        return apiClient.favouritePhoto(userId, photoName)
            .map { response ->
                val errorCode = response.errorCode as ErrorCode.FavouritePhotoErrors
                if (errorCode !is ErrorCode.FavouritePhotoErrors.Ok) {
                    return@map Either.Error(errorCode)
                }

                val galleryPhoto = galleryPhotoRepository.findByPhotoName(photoName)
                if (galleryPhoto != null) {
                    database.transactional {
                        if (!galleryPhotoRepository.favouritePhoto(galleryPhoto.galleryPhotoId)) {
                            return@transactional false
                        }

                        if (!galleryPhotoRepository.updateFavouritesCount(photoName, response.favouritesCount)) {
                            return@transactional false
                        }

                        return@transactional true
                    }

                }

                return@map Either.Value(FavouritePhotoResult(response.isFavourited, response.favouritesCount))
            }
            .toObservable()
            .doOnError { Timber.tag(TAG).e(it) }
    }

    data class FavouritePhotoResult(val isFavourited: Boolean,
                                    val favouritesCount: Long)
}