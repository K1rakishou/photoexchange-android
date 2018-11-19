package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.mvp.model.exception.ApiErrorException
import com.kirakishou.photoexchange.mvp.model.exception.BadServerResponse
import core.ErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import net.response.StatusResponse
import retrofit2.Response
import kotlin.coroutines.CoroutineContext

/**
 * Created by kirakishou on 3/17/2018.
 */
abstract class BaseRequest<T>(
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.IO()

  abstract suspend fun execute(): T

  protected fun <T : StatusResponse> handleResponse(
    jsonConverter: JsonConverter,
    response: Response<T>
  ): Either<ApiErrorException, T> {
    if (!response.isSuccessful) {
      try {
        val responseJson = response.errorBody()!!.string()
        val error = jsonConverter.fromJson<T>(responseJson, StatusResponse::class.java)

        //may happen in some rare cases (like when client and server have endpoints with different parameters)
        if (error?.errorCode == null) {
          throw BadServerResponse()
        } else {
          //server returned non-zero status
          throw ApiErrorException(ErrorCode.fromInt(error.errorCode))
        }
      } catch (e: Exception) {
        throw e
      }
    }

    return Either.Value(response.body()!!)
  }
}