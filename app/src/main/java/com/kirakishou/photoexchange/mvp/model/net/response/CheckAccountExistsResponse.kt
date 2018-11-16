package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class CheckAccountExistsResponse
private constructor(

  @Expose
  @SerializedName("account_exists")
  val accountExists: Boolean,

  errorCode: ErrorCode
) : StatusResponse(errorCode.getValue(), errorCode) {

  companion object {
    fun success(accountExists: Boolean): CheckAccountExistsResponse {
      return CheckAccountExistsResponse(accountExists, ErrorCode.CheckAccountExistsErrors.Ok())
    }

    fun fail(errorCode: ErrorCode): CheckAccountExistsResponse {
      return CheckAccountExistsResponse(false, errorCode)
    }
  }
}