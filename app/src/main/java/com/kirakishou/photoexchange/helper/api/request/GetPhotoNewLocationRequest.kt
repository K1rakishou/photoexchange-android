package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.exception.ApiException
import com.kirakishou.photoexchange.mwvm.model.net.response.GetUserLocationResponse
import com.kirakishou.photoexchange.mwvm.model.net.response.StatusResponse
import io.reactivex.Single
import timber.log.Timber

/**
 * Created by kirakishou on 1/8/2018.
 */
class GetPhotoNewLocationRequest(
        private val userId: String,
        private val photoIds: String,
        private val apiService: ApiService,
        private val schedulers: SchedulerProvider,
        private val gson: Gson
) : AbstractRequest<GetUserLocationResponse>() {

    override fun build(): Single<GetUserLocationResponse> {
        return apiService.getPhotoNewLocation(userId, photoIds)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .lift(OnApiErrorSingle(gson))
                .onErrorResumeNext { error -> convertExceptionToErrorCode(error) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertExceptionToErrorCode(error: Throwable): Single<GetUserLocationResponse> {
        val response = when (error) {
            is ApiException -> GetUserLocationResponse(emptyList(), error.serverErrorCode)

            else -> {
                Timber.d("Unknown exception")
                throw error
            }
        }

        return Single.just(response)
    }
}