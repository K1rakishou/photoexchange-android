package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse
import com.kirakishou.photoexchange.mvp.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
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
    fun uploadPhoto(@Part packet: MultipartBody.Part,
                    @Part photo: MultipartBody.Part): Single<Response<UploadPhotoResponse>>

    @GET("/v1/api/get_answer/{photo_names}/{user_id}")
    fun getPhotoAnswers(@Path("photo_names") photoNames: String,
                        @Path("user_id") userId: String): Single<Response<PhotoAnswerResponse>>

    @GET("/v1/api/get_gallery_photos/{last_id}/{count}")
    fun getGalleryPhotos(@Path("last_id") lastId: Long,
                         @Path("count") count: Int): Single<Response<GalleryPhotosResponse>>
}