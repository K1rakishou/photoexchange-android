package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.mwvm.model.exception.ApiException
import com.kirakishou.photoexchange.mwvm.model.net.response.StatusResponse
import io.reactivex.Single
import timber.log.Timber

/**
 * Created by kirakishou on 11/3/2017.
 */
abstract class AbstractRequest<T> {
    abstract fun build(): Single<T>
}