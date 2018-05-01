package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable

class FavouritePhotoUseCase(
    private val apiClient: ApiClient
) {
    private val tag = "FavouritePhotoUseCase"

    fun favouritePhoto(userId: String, photoName: String): Observable<Boolean> {
        return Observable.fromCallable {
            val response = apiClient.favouritePhoto(userId, photoName).blockingGet()
            val errorCode = response.errorCode as ErrorCode.FavouritePhotoErrors

            return@fromCallable when (errorCode) {
                is ErrorCode.FavouritePhotoErrors.Remote.Ok -> true
                else -> false
            }
        }
    }
}