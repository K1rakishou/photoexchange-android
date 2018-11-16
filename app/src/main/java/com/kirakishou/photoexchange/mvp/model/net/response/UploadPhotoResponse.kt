package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoResponse(

  @Expose
  @SerializedName("photo_id")
  val photoId: Long,

  @Expose
  @SerializedName("photo_name")
  val photoName: String,

  errorCode: ErrorCode

) : StatusResponse(errorCode.getValue(), errorCode) {

  companion object {
    fun success(photoId: Long, photoName: String): UploadPhotoResponse {
      return UploadPhotoResponse(photoId, photoName, ErrorCode.UploadPhotoErrors.Ok())
    }

    fun error(errorCode: ErrorCode): UploadPhotoResponse {
      return UploadPhotoResponse(-1L, "", errorCode)
    }
  }
}