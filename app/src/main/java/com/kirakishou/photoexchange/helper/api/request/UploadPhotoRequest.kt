package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.ProgressRequestBody
import com.kirakishou.photoexchange.helper.api.ApiService
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
        return Single.fromCallable {
            val packet = SendPhotoPacket(location.lon, location.lat, userId)
            val photoFile = File(photoFilePath)

            if (!photoFile.isFile || !photoFile.exists()) {
                return@fromCallable UploadPhotoResponse.error(ErrorCode.NO_PHOTO_FILE_ON_DISK) as T
            }

            val body = getBody(photoFile, packet, callback)

            try {
                val response = apiService.uploadPhoto(body.part(0), body.part(1))
                    .blockingGet() as Response<T>

                return@fromCallable extractResponse(response)
            } catch (error: Throwable) {
                return@fromCallable UploadPhotoResponse.error(ErrorCode.UNKNOWN_ERROR) as T
            }

        }.subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractResponse(response: Response<T>): T {
        if (!response.isSuccessful) {
            try {
                val responseJson = response.errorBody()!!.string()
                val error = gson.fromJson<StatusResponse>(responseJson, StatusResponse::class.java)

                //may happen in some rare cases
                return if (error?.serverErrorCode == null) {
                    UploadPhotoResponse.error(ErrorCode.BAD_SERVER_RESPONSE) as T
                } else {
                    UploadPhotoResponse.error(ErrorCode.from(error.serverErrorCode)) as T
                }
            } catch (e: Throwable) {
                Timber.e(e)
                return UploadPhotoResponse.error(ErrorCode.UNKNOWN_ERROR) as T
            }
        }

        return response.body()!!
    }

    private fun getBody(photoFile: File, packet: SendPhotoPacket, callback: UploadPhotosUseCase.PhotoUploadProgressCallback): MultipartBody {
        val photoRequestBody = ProgressRequestBody(photoFile, callback)
        val packetJson = gson.toJson(packet)

        return MultipartBody.Builder()
            .addFormDataPart("photo", photoFile.name, photoRequestBody)
            .addFormDataPart("packet", packetJson)
            .build()
    }
}
