package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.ProgressRequestBody
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.packet.SendPhotoPacket
import com.kirakishou.photoexchange.mvp.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import okhttp3.MultipartBody
import java.io.File
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoRequest<T>(
    private val photoFilePath: String,
    private val location: LonLat,
    private val userId: String,
    private val isPublic: Boolean,
    private val callback: UploadPhotosUseCase.PhotoUploadProgressCallback,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
) : AbstractRequest<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun execute(): Single<T> {
        val single = Single.fromCallable {
            val packet = SendPhotoPacket(location.lon, location.lat, userId, isPublic)
            val photoFile = File(photoFilePath)

            if (!photoFile.isFile || !photoFile.exists()) {
                throw NoPhotoFileOnDiskException()
            }

            return@fromCallable getBody(photoFile, packet, callback)
        }

        return single
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .flatMap { body ->
                return@flatMap apiService.uploadPhoto(body.part(0), body.part(1))
                    .lift(OnApiErrorSingle<UploadPhotoResponse>(gson, UploadPhotoResponse::class.java))
                    .map { response ->
                        if (response.serverErrorCode!! == 0) {
                            return@map UploadPhotoResponse.success(response.photoName)
                        } else {
                            return@map UploadPhotoResponse.error(ErrorCode.fromInt(ErrorCode.UploadPhotoErrors::class.java, response.serverErrorCode))
                        }
                    }
                    .onErrorReturn(this::extractError) as Single<T>
            }
    }

    private fun extractError(error: Throwable): UploadPhotoResponse {
        return when (error) {
            is ApiException -> UploadPhotoResponse.error(error.errorCode)
            is SocketTimeoutException,
            is TimeoutException -> UploadPhotoResponse.error(ErrorCode.UploadPhotoErrors.Local.Timeout())
            is NoPhotoFileOnDiskException -> UploadPhotoResponse.error(ErrorCode.UploadPhotoErrors.Local.NoPhotoFileOnDisk())
            else -> UploadPhotoResponse.error(ErrorCode.UploadPhotoErrors.Remote.UnknownError())
        }
    }

    private fun getBody(photoFile: File, packet: SendPhotoPacket, callback: UploadPhotosUseCase.PhotoUploadProgressCallback): MultipartBody {
        val photoRequestBody = ProgressRequestBody(photoFile, callback)
        val packetJson = gson.toJson(packet)

        return MultipartBody.Builder()
            .addFormDataPart("photo", photoFile.name, photoRequestBody)
            .addFormDataPart("packet", packetJson)
            .build()
    }

    class NoPhotoFileOnDiskException : Exception()
}
