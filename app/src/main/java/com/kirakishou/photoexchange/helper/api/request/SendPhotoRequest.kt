package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoWithInfo
import com.kirakishou.photoexchange.mvvm.model.exception.PhotoDoesNotExistsException
import com.kirakishou.photoexchange.mvvm.model.net.packet.SendPhotoPacket
import com.kirakishou.photoexchange.mvvm.model.net.response.StatusResponse
import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

/**
 * Created by kirakishou on 11/3/2017.
 */
class SendPhotoRequest<T : StatusResponse>(private val info: PhotoWithInfo,
                                           private val apiService: ApiService,
                                           private val gson: Gson) : AbstractRequest<Single<T>>() {

    override fun build(): Single<T> {
        return getBodySingle(info.photoFile)
                .flatMap { body ->
                    val packet = SendPhotoPacket(info.location, info.userId)

                    return@flatMap apiService.sendPhoto<T>(body, packet)
                            .lift(OnApiErrorSingle(gson))
                }
                .onErrorResumeNext { error -> convertExceptionToErrorCode(error) }
    }

    private fun getBodySingle(photoFile: File): Single<MultipartBody.Part> {
        return Single.fromCallable {
            if (!photoFile.isFile || !photoFile.exists()) {
                throw PhotoDoesNotExistsException()
            }

            val progressBody = RequestBody.create(MediaType.parse("multipart/form-data"), photoFile)
            return@fromCallable MultipartBody.Part.createFormData("photo", photoFile.name, progressBody)
        }
    }
}





















