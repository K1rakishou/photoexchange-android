package com.kirakishou.photoexchange.helper.api

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.request.UploadPhotoRequest
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.service.UploadPhotoServiceCallbacks
import kotlinx.coroutines.experimental.Deferred
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Created by kirakishou on 3/17/2018.
 */
class ApiClient
@Inject constructor(
    private val apiService: ApiService,
    private val gson: Gson,
    private val coroutinePool: CoroutineThreadPoolProvider
) {

    fun uploadPhoto(photoId: Long, photoFilePath: String, location: LonLat, userId: String,
                    callback: WeakReference<UploadPhotoServiceCallbacks>): Deferred<UploadPhotoResponse> {
        return UploadPhotoRequest<UploadPhotoResponse>(photoId, photoFilePath, location, userId, callback, apiService, coroutinePool, gson)
            .execute()
    }

//    fun findPhotoAnswer(userId: String): Single<PhotoAnswerResponse> {
//        return FindPhotoAnswerRequest(userId, apiService, schedulers, gson)
//            .execute()
//    }
//
//    fun markPhotoAsReceived(photoId: Long, userId: String): Single<StatusResponse> {
//        return MarkPhotoAsReceivedRequest(photoId, userId, apiService, schedulers, gson)
//            .execute()
//    }
//
//    fun getPhotoRecipientsLocations(userId: String, photoIds: String): Single<GetUserLocationResponse> {
//        return GetPhotoNewLocationRequest(userId, photoIds, apiService, schedulers, gson)
//            .execute()
//    }

}