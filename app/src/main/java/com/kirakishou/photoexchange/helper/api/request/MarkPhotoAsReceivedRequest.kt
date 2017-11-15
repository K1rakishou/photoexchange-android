package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.net.response.StatusResponse
import io.reactivex.Single

/**
 * Created by kirakishou on 11/14/2017.
 */
class MarkPhotoAsReceivedRequest(
        private val photoId: Long,
        private val userId: String,
        private val apiService: ApiService,
        private val schedulers: SchedulerProvider,
        private val gson: Gson
) : AbstractRequest<Single<StatusResponse>>() {

    override fun build(): Single<StatusResponse> {
        return apiService.markPhotoAsReceived(photoId, userId)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .lift(OnApiErrorSingle(gson))
                .onErrorResumeNext { error -> convertExceptionToErrorCode(error) }
    }
}