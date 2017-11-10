package com.kirakishou.photoexchange.helper.api

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.request.SendPhotoRequest
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoToBeUploaded
import com.kirakishou.photoexchange.mvvm.model.net.response.UploadPhotoResponse
import io.reactivex.Single
import javax.inject.Inject


/**
 * Created by kirakishou on 7/22/2017.
 */

class ApiClientImpl
@Inject constructor(protected val apiService: ApiService,
                    protected val gson: Gson,
                    protected val schedulers: SchedulerProvider) : ApiClient {

    override fun sendPhoto(info: PhotoToBeUploaded): Single<UploadPhotoResponse> {
        return SendPhotoRequest(info, apiService, gson)
                .build()
    }
}




































