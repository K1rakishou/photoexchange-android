package com.kirakishou.photoexchange.helper.concurrency.rx.operator

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.gson.MyGson
import com.kirakishou.photoexchange.mvp.model.exception.GeneralException
import com.kirakishou.photoexchange.mvp.model.net.response.*
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.SingleObserver
import io.reactivex.SingleOperator
import io.reactivex.disposables.Disposable
import retrofit2.Response
import kotlin.reflect.KClass

/**
 * Created by kirakishou on 8/25/2017.
 */

/**
 * Whenever HttpException occurs this class returns converted errorBody as an ApiException with ErrorCode
 * Or ApiException with BadServerResponse and HttpStatus when server didn't return any errorCode in JSON
 * */
class OnApiErrorSingle<T : StatusResponse>(
    val gson: MyGson,
    val clazz: KClass<*>
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

                        //may happen in some rare cases (like when client and server have endpoints with different parameters)
                        if (error?.serverErrorCode == null) {
                            observer.onError(GeneralException.ApiException(getBadErrorCodeByClass(clazz)))
                        } else {
                            observer.onError(GeneralException.ApiException(getErrorCode(error.serverErrorCode!!)))
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
        return ErrorCode.fromInt(clazz, errorCodeInt)!!
    }

    //TODO: don't forget to add errorCodes here
    private fun getBadErrorCodeByClass(clazz: KClass<*>): ErrorCode {
        val errorCode = when (clazz) {
            UploadPhotoResponse::class -> ErrorCode.UploadPhotoErrors.LocalBadServerResponse()
            ReceivedPhotosResponse::class -> ErrorCode.ReceivePhotosErrors.LocalBadServerResponse()
            GalleryPhotoIdsResponse::class -> ErrorCode.GetGalleryPhotosErrors.LocalBadServerResponse()
            FavouritePhotoResponse::class -> ErrorCode.FavouritePhotoErrors.LocalBadServerResponse()
            ReportPhotoResponse::class -> ErrorCode.ReportPhotoErrors.LocalBadServerResponse()
            GetUserIdResponse::class -> ErrorCode.GetUserIdError.LocalBadServerResponse()
            GetUploadedPhotosResponse::class -> ErrorCode.GetUploadedPhotosErrors.LocalBadServerResponse()
            else -> throw IllegalArgumentException("Bad class: $clazz")
        }

        return errorCode
    }
}