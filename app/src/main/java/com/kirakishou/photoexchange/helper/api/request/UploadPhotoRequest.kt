package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.ProgressRequestBody
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.packet.SendPhotoPacket
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.model.other.LonLat
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
  private val jsonConverter: JsonConverter
) : BaseRequest<T>() {

  private val tag = "UploadPhotoRequest"

  @Suppress("UNCHECKED_CAST")
  override fun execute(): Single<T> {
    val single = Single.fromCallable {
      val packet = SendPhotoPacket(location.lon, location.lat, userId, isPublic)
      val photoFile = File(photoFilePath)

      return@fromCallable getBody(photoFile, packet, callback)
    }

    return single
      .subscribeOn(schedulerProvider.IO())
      .observeOn(schedulerProvider.IO())
      .flatMap { body ->
        return@flatMap apiService.uploadPhoto(body.part(0), body.part(1))
          .lift(OnApiErrorSingle<UploadPhotoResponse>(jsonConverter, UploadPhotoResponse::class))
          .map { response ->
            if (ErrorCode.UploadPhotoErrors.fromInt(response.serverErrorCode!!) is ErrorCode.UploadPhotoErrors.Ok) {
              return@map UploadPhotoResponse.success(response.photoId, response.photoName)
            } else {
              return@map UploadPhotoResponse.error(ErrorCode.fromInt(ErrorCode.UploadPhotoErrors::class, response.serverErrorCode!!))
            }
          }
          .onErrorReturn(this::extractError) as Single<T>
      }
  }

  private fun extractError(error: Throwable): UploadPhotoResponse {
    return when (error) {
      is ApiException -> UploadPhotoResponse.error(error.errorCode)
      is SocketTimeoutException,
      is TimeoutException -> UploadPhotoResponse.error(ErrorCode.UploadPhotoErrors.LocalTimeout())
      else -> UploadPhotoResponse.error(ErrorCode.UploadPhotoErrors.UnknownError())
    }
  }

  private fun getBody(photoFile: File, packet: SendPhotoPacket, callback: UploadPhotosUseCase.PhotoUploadProgressCallback): MultipartBody {
    val photoRequestBody = ProgressRequestBody(photoFile, callback)
    val packetJson = jsonConverter.toJson(packet)

    return MultipartBody.Builder()
      .addFormDataPart("photo", photoFile.name, photoRequestBody)
      .addFormDataPart("packet", packetJson)
      .build()
  }
}
