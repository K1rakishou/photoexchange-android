package com.kirakishou.photoexchange.mvp.model.exception

import com.kirakishou.photoexchange.mvp.model.other.ServerErrorCode

/**
 * Created by kirakishou on 11/7/2017.
 */
class UnknownErrorCodeException(errorCode: ServerErrorCode) : Exception("Unknown error code $errorCode")