package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GetReceivedPhotosResponse
private constructor(

  @Expose
  @SerializedName("received_photos")
  val receivedPhotos: List<ReceivedPhoto>,

  errorCode: ErrorCode
) : StatusResponse(errorCode.getValue(), errorCode) {

  companion object {
    fun success(uploadedPhotos: List<ReceivedPhoto>): GetReceivedPhotosResponse {
      return GetReceivedPhotosResponse(uploadedPhotos, ErrorCode.GetReceivedPhotosErrors.Ok())
    }

    fun fail(errorCode: ErrorCode): GetReceivedPhotosResponse {
      return GetReceivedPhotosResponse(emptyList(), errorCode)
    }
  }

  class ReceivedPhoto(

    @Expose
    @SerializedName("photo_id")
    val photoId: Long,

    @Expose
    @SerializedName("uploaded_photo_name")
    val uploadedPhotoName: String,

    @Expose
    @SerializedName("received_photo_name")
    val receivedPhotoName: String,

    @Expose
    @SerializedName("receiver_lon")
    val receiverLon: Double,

    @Expose
    @SerializedName("receiver_lat")
    val receiverLat: Double
  )
}