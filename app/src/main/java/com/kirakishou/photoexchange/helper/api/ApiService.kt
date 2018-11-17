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

  @GET("/v1/api/receive_photos/{photo_names}/{user_id}")
  fun receivePhotos(@Path("photo_names") photoNames: String,
                    @Path("user_id") userId: String): Single<Response<ReceivedPhotosResponse>>

  @GET("/v1/api/get_user_id")
  fun getUserId(): Single<Response<GetUserIdResponse>>

  @GET("/v1/api/get_gallery_photo_info/{user_id}/{photo_ids}")
  fun getGalleryPhotoInfo(@Path("user_id") userId: String,
                          @Path("photo_ids") photoIds: String): Single<Response<GalleryPhotoInfoResponse>>

  @GET("/v1/api/get_page_of_gallery_photos/{last_uploaded_on}/{count}")
  fun getPageOfGalleryPhotos(@Path("last_uploaded_on") lastUploadedOn: Long,
                             @Path("count") count: Int): Single<Response<GalleryPhotosResponse>>

  @GET("/v1/api/get_page_of_uploaded_photos/{user_id}/{last_uploaded_on}/{count}")
  fun getPageOfUploadedPhotos(@Path("user_id") userId: String,
                              @Path("last_uploaded_on") lastUploadedOn: Long,
                              @Path("count") count: Int): Single<Response<GetUploadedPhotosResponse>>

  @GET("/v1/api/get_page_of_received_photos/{user_id}/{last_uploaded_on}/{count}")
  fun getReceivedPhotos(@Path("user_id") userId: String,
                        @Path("last_uploaded_on") lastUploadedOn: Long,
                        @Path("count") count: Int): Single<Response<GetReceivedPhotosResponse>>

  @GET("/v1/api/check_account_exists/{user_id}")
  fun checkAccountExists(@Path("user_id") userId: String): Single<Response<CheckAccountExistsResponse>>
}