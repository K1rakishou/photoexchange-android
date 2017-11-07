package com.kirakishou.photoexchange.mvvm.model.exception

import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode

/**
 * Created by kirakishou on 11/7/2017.
 */
class UnknownErrorCodeException(errorCode: ServerErrorCode) : Exception("Unknown error code $errorCode")