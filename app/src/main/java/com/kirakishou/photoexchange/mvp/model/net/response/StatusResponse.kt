package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

/**
 * Created by kirakishou on 3/3/2018.
 */
open class StatusResponse(
  @Expose
  @SerializedName("server_error_code")
  var serverErrorCode: Int?,

  var errorCode: ErrorCode
) {
  companion object {
    fun fromErrorCode(errorCode: ErrorCode): StatusResponse {
      return StatusResponse(errorCode.getValue(), errorCode)
    }
  }
}