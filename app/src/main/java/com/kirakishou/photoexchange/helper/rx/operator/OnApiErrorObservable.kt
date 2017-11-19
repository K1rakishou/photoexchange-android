package com.kirakishou.photoexchange.helper.rx.operator

import com.google.gson.Gson
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import com.kirakishou.photoexchange.mwvm.model.exception.ApiException
import com.kirakishou.photoexchange.mwvm.model.exception.BadServerResponseException
import com.kirakishou.photoexchange.mwvm.model.net.response.StatusResponse
import io.reactivex.ObservableOperator
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import retrofit2.Response
import timber.log.Timber

/**
 * Created by kirakishou on 8/27/2017.
 */
class OnApiErrorObservable<T : StatusResponse>(val gson: Gson) : ObservableOperator<T, Response<T>> {

    override fun apply(observer: Observer<in T>): Observer<in Response<T>> {
        return object : Observer<Response<T>> {

            override fun onSubscribe(d: Disposable) {
                observer.onSubscribe(d)
            }

            override fun onNext(response: Response<T>) {
                if (!response.isSuccessful) {
                    try {
                        val responseJson = response.errorBody()!!.string()
                        val error = gson.fromJson<StatusResponse>(responseJson, StatusResponse::class.java)
                        Timber.d(responseJson)

                        //may happen in some rare cases
                        if (error.serverErrorCode != null) {
                            observer.onError(ApiException(ServerErrorCode.from(error.serverErrorCode)))
                        } else {
                            observer.onError(BadServerResponseException())
                        }
                    } catch (e: Exception) {
                        observer.onError(e)
                    }
                } else {
                    observer.onNext(response.body()!!)
                }
            }

            override fun onError(e: Throwable) {
                observer.onError(e)
            }

            override fun onComplete() {
                observer.onComplete()
            }
        }
    }
}