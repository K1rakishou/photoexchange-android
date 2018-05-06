package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import timber.log.Timber

class ReportPhotoUseCase(
    private val apiClient: ApiClient
) {
    private val TAG = "ReportPhotoUseCase"

    fun reportPhoto(userId: String, photoName: String): Observable<Either<ErrorCode, Boolean>> {
        return apiClient.reportPhoto(userId, photoName)
            .map { response ->
                val errorCode = response.errorCode
                if (errorCode !is ErrorCode.ReportPhotoErrors.Remote.Ok) {
                    return@map Either.Error(errorCode)
                }

                return@map Either.Value(response.isReported)
            }
            .toObservable()
            .doOnError { Timber.tag(TAG).e(it) }
    }
}