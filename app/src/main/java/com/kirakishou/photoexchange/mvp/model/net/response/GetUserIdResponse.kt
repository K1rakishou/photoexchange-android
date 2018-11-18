package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GetUserIdResponse
private constructor(

  @Expose
  @SerializedName("user_id")
  val userId: String,

  errorCode: ErrorCode
) : StatusResponse(errorCode.value, errorCode) {

  companion object {
    fun success(userId: String): GetUserIdResponse {
      return GetUserIdResponse(userId, ErrorCode.Ok)
    }

    fun error(errorCode: ErrorCode): GetUserIdResponse {
      return GetUserIdResponse("", errorCode)
    }
  }
}