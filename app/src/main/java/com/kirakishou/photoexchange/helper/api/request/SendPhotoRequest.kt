package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoToBeUploaded
import com.kirakishou.photoexchange.mwvm.model.exception.PhotoDoesNotExistsException
import com.kirakishou.photoexchange.mwvm.model.net.packet.SendPhotoPacket
import com.kirakishou.photoexchange.mwvm.model.net.response.UploadPhotoResponse
import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 11/3/2017.
 */
class SendPhotoRequest(
        private val info: PhotoToBeUploaded,
        private val apiService: ApiService,
        private val schedulers: SchedulerProvider,
        private val gson: Gson
) : AbstractRequest<Single<UploadPhotoResponse>>() {

    override fun build(): Single<UploadPhotoResponse> {
        val packet = SendPhotoPacket(info.location.lon, info.location.lat, info.userId)

        return getBodySingle(info.photoFilePath, packet)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .flatMap { multipartBody ->
                    return@flatMap apiService.sendPhoto(multipartBody.part(0), multipartBody.part(1))
                            .lift(OnApiErrorSingle(gson))
                }
                .delay(3, TimeUnit.SECONDS)
                .onErrorResumeNext { error -> convertExceptionToErrorCode(error) }
    }

    private fun getBodySingle(photoFilePath: String, packet: SendPhotoPacket): Single<MultipartBody> {
        return Single.fromCallable {
            val photoFile = File(photoFilePath)

            if (!photoFile.isFile || !photoFile.exists()) {
                throw PhotoDoesNotExistsException()
            }

            val photoRequestBody = RequestBody.create(MediaType.parse("image/*"), photoFile)
            val packetJson = gson.toJson(packet)

            return@fromCallable MultipartBody.Builder()
                    .addFormDataPart("photo", photoFile.name, photoRequestBody)
                    .addFormDataPart("packet", packetJson)
                    .build()
        }
    }
}





















