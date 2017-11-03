package com.kirakishou.photoexchange.helper.api

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.request.SendPhotoRequest
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoWithLocation
import com.kirakishou.photoexchange.mvvm.model.net.response.SendPhotoResponse
import com.kirakishou.photoexchange.mvvm.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mvvm.viewmodel.MainActivityViewModel
import io.reactivex.Single
import javax.inject.Inject


/**
 * Created by kirakishou on 7/22/2017.
 */

class ApiClientImpl
@Inject constructor(protected val apiService: ApiService,
                    protected val gson: Gson,
                    protected val schedulers: SchedulerProvider) : ApiClient {

    override fun <T : StatusResponse> sendPhoto(info: PhotoWithLocation): Single<T> {
        return SendPhotoRequest<T>(info, apiService, gson)
                .build()
    }
}




































