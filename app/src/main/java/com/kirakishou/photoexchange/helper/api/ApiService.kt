package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.mvvm.model.net.packet.SendPhotoPacket
import com.kirakishou.photoexchange.mvvm.model.net.response.SendPhotoResponse
import io.reactivex.Single
import okhttp3.MultipartBody
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
    fun <T> sendPhoto(@Part photo: MultipartBody.Part,
                  @Part("packet") packet: SendPhotoPacket) : Single<Response<T>>
}





































