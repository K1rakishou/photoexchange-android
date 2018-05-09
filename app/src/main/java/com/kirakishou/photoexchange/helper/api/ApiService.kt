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

    @PUT("/v1/api/favourite")
    fun favouritePhoto(@Body packet: FavouritePhotoPacket): Single<Response<FavouritePhotoResponse>>

    @PUT("/v1/api/report")
    fun reportPhoto(@Body packet: ReportPhotoPacket): Single<Response<ReportPhotoResponse>>

    @GET("/v1/api/get_answer/{photo_names}/{user_id}")
    fun getPhotoAnswers(@Path("photo_names") photoNames: String,
                        @Path("user_id") userId: String): Single<Response<PhotoAnswerResponse>>

    @GET("/v1/api/get_gallery_photo_ids/{last_id}/{count}")
    fun getGalleryPhotoIds(@Path("last_id") lastId: Long,
                           @Path("count") count: Int): Single<Response<GalleryPhotoIdsResponse>>

    @GET("/v1/api/get_gallery_photos/{photo_ids}")
    fun getGalleryPhotos(@Path("photo_ids") photoIds: String): Single<Response<GalleryPhotosResponse>>

    @GET("/v1/api/get_gallery_photo_info/{user_id}/{photo_ids}")
    fun getGalleryPhotoInfo(@Path("user_id") userId: String,
                            @Path("photo_ids") photoIds: String): Single<Response<GalleryPhotoInfoResponse>>

    @GET("/v1/api/get_user_id")
    fun getUserId(): Single<Response<GetUserIdResponse>>
}