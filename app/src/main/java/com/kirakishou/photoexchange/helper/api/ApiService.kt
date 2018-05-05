package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.mvp.model.net.packet.FavouritePhotoPacket
import com.kirakishou.photoexchange.mvp.model.net.packet.ReportPhotoPacket
import com.kirakishou.photoexchange.mvp.model.net.response.*
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

    @GET("/v1/api/get_gallery_photos/{user_id}/{last_id}/{count}")
    fun getGalleryPhotos(@Path("user_id") userId: String,
                         @Path("last_id") lastId: Long,
                         @Path("count") count: Int): Single<Response<GalleryPhotosResponse>>

    @PUT("/v1/api/favourite")
    fun favouritePhoto(@Body packet: FavouritePhotoPacket): Single<Response<FavouritePhotoResponse>>

    @PUT("/v1/api/report")
    fun reportPhoto(@Body packet: ReportPhotoPacket): Single<Response<ReportPhotoResponse>>
}