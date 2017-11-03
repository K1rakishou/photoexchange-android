package com.kirakishou.photoexchange.mvvm.model.exception

import com.kirakishou.photoexchange.mvvm.model.ErrorCode


/**
 * Created by kirakishou on 8/25/2017.
 */
class ApiException(val errorCode: ErrorCode) : Exception()