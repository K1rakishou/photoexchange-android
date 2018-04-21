package com.kirakishou.photoexchange.helper.concurrency.rx.operator

import com.google.gson.Gson
import com.kirakishou.photoexchange.mvp.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.SingleObserver
import io.reactivex.SingleOperator
import io.reactivex.disposables.Disposable
import retrofit2.Response
import timber.log.Timber

/**
 * Created by kirakishou on 8/25/2017.
 */

/**
 *
 * Whenever HttpException occurs this class returns converted errorBody as an ApiException with ErrorCode and HttpStatus
 *
 * */
class OnApiErrorSingle<T : StatusResponse>(
    val gson: Gson,
    val clazz: Class<*>
) : SingleOperator<T, Response<T>> {

    override fun apply(observer: SingleObserver<in T>): SingleObserver<in Response<T>> {
        return object : SingleObserver<Response<T>> {
            override fun onError(e: Throwable) {
                observer.onError(e)
            }

            override fun onSubscribe(d: Disposable) {
                observer.onSubscribe(d)
            }

            override fun onSuccess(response: Response<T>) {
                if (!response.isSuccessful) {
                    try {
                        val responseJson = response.errorBody()!!.string()
                        val error = gson.fromJson<T>(responseJson, StatusResponse::class.java)
                        Timber.d(responseJson)

                        //may happen in some rare cases
                        if (error?.serverErrorCode == null) {
                            observer.onSuccess(StatusResponse.fromErrorCode(getBadErrorCodeByClass(clazz)) as T)
                        } else {
                            observer.onSuccess(StatusResponse.fromErrorCode(getErrorCode(error.serverErrorCode!!)) as T)
                        }
                    } catch (e: Exception) {
                        observer.onError(e)
                    }
                } else {
                    observer.onSuccess(response.body()!!)
                }
            }
        }
    }

    private fun getErrorCode(errorCodeInt: Int): ErrorCode {
        return ErrorCode.fromInt(errorCodeInt)
    }

    private fun getBadErrorCodeByClass(clazz: Class<*>): ErrorCode {
        return when (clazz) {
            is ErrorCode.UploadPhotoErrors -> ErrorCode.UploadPhotoErrors.BadServerResponse()
            is ErrorCode.GetPhotoAnswerErrors -> ErrorCode.GetPhotoAnswerErrors.BadServerResponse()
            is ErrorCode.MarkPhotoAsReceivedErrors -> ErrorCode.MarkPhotoAsReceivedErrors.BadServerResponse()
            else -> throw IllegalArgumentException("Bad class: $clazz")
        }
    }
}