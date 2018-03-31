package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.ProgressRequestBody
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.net.packet.SendPhotoPacket
import com.kirakishou.photoexchange.mvp.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.model.other.ServerErrorCode
import com.kirakishou.photoexchange.service.UploadPhotoServiceCallbacks
import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoRequest<T : StatusResponse>(
    private val photoId: Long,
    private val photoFilePath: String,
    private val location: LonLat,
    private val userId: String,
    private val callback: WeakReference<UploadPhotoServiceCallbacks>,
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
                return@fromCallable UploadPhotoResponse.error(ServerErrorCode.NO_PHOTO_FILE_ON_DISK) as T
            }

            val body = getBody(photoId, photoFile, packet, callback)
            return@fromCallable extractResponse(apiService.uploadPhoto(body.part(0), body.part(1)).blockingGet() as Response<T>)
        }.subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractResponse(response: Response<T>): T {
        if (!response.isSuccessful) {
            try {
                val responseJson = response.errorBody()!!.string()
                val error = gson.fromJson<StatusResponse>(responseJson, StatusResponse::class.java)
                Timber.d(responseJson)

                //may happen in some rare cases
                return if (error?.serverErrorCode == null) {
                    UploadPhotoResponse.error(ServerErrorCode.BAD_SERVER_RESPONSE) as T
                } else {
                    UploadPhotoResponse.error(ServerErrorCode.from(error.serverErrorCode)) as T
                }
            } catch (e: Throwable) {
                Timber.e(e)
                return UploadPhotoResponse.error(ServerErrorCode.UNKNOWN_ERROR) as T
            }
        }

        return response.body()!!
    }

    private fun getBody(photoId: Long, photoFile: File, packet: SendPhotoPacket, callback: WeakReference<UploadPhotoServiceCallbacks>): MultipartBody {
        val photoRequestBody = ProgressRequestBody(photoId, photoFile, callback)
        val packetJson = gson.toJson(packet)

        return MultipartBody.Builder()
            .addFormDataPart("photo", photoFile.name, photoRequestBody)
            .addFormDataPart("packet", packetJson)
            .build()
    }
}
