package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.mwvm.model.net.response.GetUserLocationResponse
import com.kirakishou.photoexchange.mwvm.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.mwvm.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mwvm.model.net.response.UploadPhotoResponse
import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Created by kirakishou on 7/22/2017.
 */
interface ApiService {

    @Multipart
    @POST("/v1/api/upload")
    fun sendPhoto(@Part packet: MultipartBody.Part,
                  @Part photo: MultipartBody.Part): Single<Response<UploadPhotoResponse>>

    @GET("/v1/api/get_answer/{user_id}")
    fun findPhotoAnswer(@Path("user_id") userId: String): Single<Response<PhotoAnswerResponse>>

    @POST("/v1/api/received/{photo_id}/{user_id}")
    fun markPhotoAsReceived(@Path("photo_id") photoId: Long,
                            @Path("user_id") userId: String): Single<Response<StatusResponse>>

    @GET("/v1/api/get_location")
    fun getPhotoNewLocation(@Query("user_id") userId: String,
                            @Query("photo_names") photoIds: String): Single<Response<GetUserLocationResponse>>
}





































