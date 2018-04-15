package com.kirakishou.photoexchange.mvp.model.exception

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

/**
 * Created by kirakishou on 11/7/2017.
 */
class UnknownErrorCodeException(errorCode: ErrorCode) : Exception("Unknown error code $errorCode")