package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.mvvm.model.net.packet.SendPhotoPacket
import com.kirakishou.photoexchange.mvvm.model.net.response.SendPhotoResponse
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Created by kirakishou on 7/22/2017.
 */
interface ApiService {

    @Multipart
    @POST("/v1/api/upload")
    fun sendPhoto(@Part packet: MultipartBody.Part,
                  @Part photo: MultipartBody.Part) : Single<Response<SendPhotoResponse>>
}





































