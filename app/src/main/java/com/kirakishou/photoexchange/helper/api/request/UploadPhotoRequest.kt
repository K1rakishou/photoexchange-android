package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoToBeUploaded
import com.kirakishou.photoexchange.mwvm.model.exception.ApiException
import com.kirakishou.photoexchange.mwvm.model.exception.PhotoDoesNotExistsException
import com.kirakishou.photoexchange.mwvm.model.net.packet.SendPhotoPacket
import com.kirakishou.photoexchange.mwvm.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mwvm.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 11/3/2017.
 */
class UploadPhotoRequest(
        private val info: PhotoToBeUploaded,
        private val apiService: ApiService,
        private val schedulers: SchedulerProvider,
        private val gson: Gson
) : AbstractRequest<UploadPhotoResponse>() {

    override fun build(): Single<UploadPhotoResponse> {
        val packet = SendPhotoPacket(info.location.lon, info.location.lat, info.userId)

        return getBodySingle(info.photoFilePath, packet)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .doOnSuccess {
                    throw ApiException(ServerErrorCode.UNKNOWN_ERROR)
                }
                .flatMap { multipartBody ->
                    return@flatMap apiService.sendPhoto(multipartBody.part(0), multipartBody.part(1))
                            .lift(OnApiErrorSingle(gson))
                }
                .delay(3, TimeUnit.SECONDS)
                .onErrorResumeNext { error -> convertExceptionToErrorCode(error) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertExceptionToErrorCode(error: Throwable): Single<UploadPhotoResponse> {
        val response = when (error) {
            is ApiException -> UploadPhotoResponse.error(error.serverErrorCode)

            else -> {
                Timber.d("Unknown exception")
                throw error
            }
        }

        return Single.just(response)
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





















