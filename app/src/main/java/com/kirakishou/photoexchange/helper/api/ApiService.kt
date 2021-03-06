package com.kirakishou.photoexchange.helper.api

import io.reactivex.Single
import net.request.FavouritePhotoPacket
import net.request.ReportPhotoPacket
import net.request.UpdateFirebaseTokenPacket
import net.response.*
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

  @GET("/v1/api/receive_photos/{user_uuid}/{photo_names}")
  fun receivePhotos(@Path("user_uuid") userUuid: String,
                    @Path("photo_names") photoNames: String): Single<Response<ReceivedPhotosResponse>>

  @GET("/v1/api/get_user_uuid")
  fun getUserUuid(): Single<Response<GetUserUuidResponse>>

  @GET("/v1/api/get_page_of_gallery_photos/{last_uploaded_on}/{count}")
  fun getPageOfGalleryPhotos(@Path("last_uploaded_on") lastUploadedOn: Long,
                             @Path("count") count: Int): Single<Response<GalleryPhotosResponse>>

  @GET("/v1/api/get_page_of_uploaded_photos/{user_uuid}/{last_uploaded_on}/{count}")
  fun getPageOfUploadedPhotos(@Path("user_uuid") userUuid: String,
                              @Path("last_uploaded_on") lastUploadedOn: Long,
                              @Path("count") count: Int): Single<Response<GetUploadedPhotosResponse>>

  @GET("/v1/api/get_page_of_received_photos/{user_uuid}/{last_uploaded_on}/{count}")
  fun getReceivedPhotos(@Path("user_uuid") userUuid: String,
                        @Path("last_uploaded_on") lastUploadedOn: Long,
                        @Path("count") count: Int): Single<Response<ReceivedPhotosResponse>>

  @GET("/v1/api/get_photos_additional_info/{user_uuid}/{photo_names}")
  fun getPhotosAdditionalInfo(@Path("user_uuid") userUuid: String,
                              @Path("photo_names") photoNames: String): Single<Response<GetPhotosAdditionalInfoResponse>>

  @GET("/v1/api/check_account_exists/{user_uuid}")
  fun checkAccountExists(@Path("user_uuid") userUuid: String): Single<Response<CheckAccountExistsResponse>>

  @POST("/v1/api/update_token")
  fun updateFirebaseToken(@Body packet: UpdateFirebaseTokenPacket): Single<Response<UpdateFirebaseTokenResponse>>

  @GET("/v1/api/get_fresh_uploaded_photos_count/{user_uuid}/{time}")
  fun getFreshUploadedPhotosCount(@Path("user_uuid") userUuid: String,
                                  @Path("time") time: Long): Single<Response<GetFreshPhotosCountResponse>>

  @GET("/v1/api/get_fresh_received_photos_count/{user_uuid}/{time}")
  fun getFreshReceivedPhotosCount(@Path("user_uuid") userUuid: String,
                                 @Path("time") time: Long): Single<Response<GetFreshPhotosCountResponse>>

  @GET("/v1/api/get_fresh_gallery_photos_count/{time}")
  fun getFreshGalleryPhotosCount(@Path("time") time: Long): Single<Response<GetFreshPhotosCountResponse>>
}