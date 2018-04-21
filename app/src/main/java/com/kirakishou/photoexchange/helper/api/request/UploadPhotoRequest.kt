package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.ProgressRequestBody
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.net.packet.SendPhotoPacket
import com.kirakishou.photoexchange.mvp.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.Response
import timber.log.Timber
import java.io.File

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoRequest<T : StatusResponse>(
    private val photoFilePath: String,
    private val location: LonLat,
    private val userId: String,
    private val callback: UploadPhotosUseCase.PhotoUploadProgressCallback,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
) : AbstractRequest<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun execute(): Single<T> {
        val single = Single.fromCallable {
            val packet = SendPhotoPacket(location.lon, location.lat, userId)
            val photoFile = File(photoFilePath)

            if (!photoFile.isFile || !photoFile.exists()) {
                throw NoPhotoFileOnDiskException()
            }

            return@fromCallable getBody(photoFile, packet, callback)
        }

        return single
            .subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
            .flatMap { body ->
                return@flatMap apiService.uploadPhoto(body.part(0), body.part(1))
                    .lift(OnApiErrorSingle<UploadPhotoResponse>(gson, UploadPhotoResponse::class.java)) as Single<T>
            }
            .onErrorReturn { throwable ->
                return@onErrorReturn when (throwable) {
                    is NoPhotoFileOnDiskException -> UploadPhotoResponse.error(ErrorCode.UploadPhotoErrors.NoPhotoFileOnDisk()) as T
                    else -> UploadPhotoResponse.error(ErrorCode.UploadPhotoErrors.UnknownError()) as T
                }
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
