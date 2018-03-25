package com.kirakishou.photoexchange.helper.concurrency.operator

import com.google.gson.Gson
import com.kirakishou.photoexchange.mvp.model.other.ServerErrorCode
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.exception.BadServerResponseException
import com.kirakishou.photoexchange.mvp.model.net.response.StatusResponse
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
 * Whenever HttpException occurs returns converted errorBody as an ApiException with ServerErrorCode.Remote and HttpStatus
 *
 * */
class OnApiErrorSingle<T>(val gson: Gson) : SingleOperator<T, Response<T>> {

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
                        val error = gson.fromJson<StatusResponse>(responseJson, StatusResponse::class.java)
                        Timber.d(responseJson)

                        //may happen in some rare cases
                        if (error?.serverErrorCode == null) {
                            observer.onError(BadServerResponseException())
                        } else {
                            observer.onError(ApiException(ServerErrorCode.from(error.serverErrorCode)))
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
}