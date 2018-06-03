package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.exception.GeneralException
import com.kirakishou.photoexchange.mvp.model.net.packet.FavouritePhotoPacket
import com.kirakishou.photoexchange.mvp.model.net.response.FavouritePhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class FavouritePhotoRequest<T>(
    private val userId: String,
    private val photoName: String,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
) : AbstractRequest<T>() {

    override fun execute(): Single<T> {
        return apiService.favouritePhoto(FavouritePhotoPacket(userId, photoName))
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<FavouritePhotoResponse>(gson, FavouritePhotoResponse::class))
            .map { response ->
                if (ErrorCode.FavouritePhotoErrors.fromInt(response.serverErrorCode!!) is ErrorCode.FavouritePhotoErrors.Ok) {
                    return@map FavouritePhotoResponse.success(response.isFavourited, response.favouritesCount)
                } else {
                    return@map FavouritePhotoResponse.error(ErrorCode.fromInt(ErrorCode.FavouritePhotoErrors::class, response.serverErrorCode!!))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): FavouritePhotoResponse {
        return when (error) {
            is GeneralException.ApiException -> FavouritePhotoResponse.error(error.errorCode as ErrorCode.FavouritePhotoErrors)
            is SocketTimeoutException,
            is TimeoutException -> FavouritePhotoResponse.error(ErrorCode.FavouritePhotoErrors.LocalTimeout())
            else -> FavouritePhotoResponse.error(ErrorCode.FavouritePhotoErrors.UnknownError())
        }
    }
}