package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.mvvm.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.mvvm.model.net.response.UploadPhotoResponse
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
}





































