package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.ProgressRequestBody
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.mvp.model.exception.ConnectionError
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.rx2.await
import net.request.SendPhotoPacket
import net.response.UploadPhotoResponse
import okhttp3.MultipartBody
import java.io.File

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoRequest(
  private val photoFilePath: String,
  private val location: LonLat,
  private val userId: String,
  private val isPublic: Boolean,
  private val photo: TakenPhoto,
  private val channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  dispatchersProvider: DispatchersProvider
) : BaseRequest<UploadPhotoResponse>(dispatchersProvider) {
  private val TAG = "UploadPhotoRequest"

  @Suppress("UNCHECKED_CAST")
  override suspend fun execute(): UploadPhotoResponse {
    val packet = SendPhotoPacket(location.lon, location.lat, userId, isPublic)
    val photoFile = File(photoFilePath)
    val body = getBody(photoFile, packet, photo, channel)

    val response = try {
      apiService.uploadPhoto(body.part(0), body.part(1)).await()
    } catch (error: Exception) {
      throw ConnectionError(error.message)
    }

    val result = handleResponse(jsonConverter, response)
    return when (result) {
      is Either.Value -> result.value
      is Either.Error -> throw result.error
    }
  }

  private fun getBody(
    photoFile: File,
    packet: SendPhotoPacket,
    photo: TakenPhoto,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ): MultipartBody {
    val photoRequestBody = ProgressRequestBody(photoFile, photo, channel)
    val packetJson = jsonConverter.toJson(packet)

    return MultipartBody.Builder()
      .addFormDataPart("photo", photoFile.name, photoRequestBody)
      .addFormDataPart("packet", packetJson)
      .build()
  }
}
