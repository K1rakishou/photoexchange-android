package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import timber.log.Timber

class FavouritePhotoUseCase(
    private val apiClient: ApiClient
) {
    private val TAG = "FavouritePhotoUseCase"

    fun favouritePhoto(userId: String, photoName: String): Observable<UseCaseResult<FavouritePhotoResult>> {
        return apiClient.favouritePhoto(userId, photoName)
            .map { response ->
                val errorCode = response.errorCode as ErrorCode.FavouritePhotoErrors

                val result = when (errorCode) {
                    is ErrorCode.FavouritePhotoErrors.Remote.Ok -> UseCaseResult.Result(FavouritePhotoResult(response.isFavourited, response.favouritesCount))
                    else -> UseCaseResult.Error(errorCode)
                }

                return@map result as UseCaseResult<FavouritePhotoResult>
            }
            .toObservable()
            .doOnError { Timber.tag(TAG).e(it) }
    }

    data class FavouritePhotoResult(val isFavourited: Boolean,
                                    val favouritesCount: Long)
}