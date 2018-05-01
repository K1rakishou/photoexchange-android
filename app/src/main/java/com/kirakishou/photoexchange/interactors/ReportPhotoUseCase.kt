package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable

class ReportPhotoUseCase(
    private val apiClient: ApiClient
) {
    private val tag = "ReportPhotoUseCase"

    fun reportPhoto(userId: String, photoName: String): Observable<Boolean> {
        return Observable.fromCallable {
            val response = apiClient.reportPhoto(userId, photoName).blockingGet()
            val errorCode = response.errorCode as ErrorCode.ReportPhotoErrors

            return@fromCallable when (errorCode) {
                is ErrorCode.ReportPhotoErrors.Remote.Ok -> response.isReported
                else -> false
            }
        }
    }
}