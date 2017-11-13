package com.kirakishou.photoexchange.mwvm.model.exception

import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode


/**
 * Created by kirakishou on 8/25/2017.
 */
class ApiException(val serverErrorCode: ServerErrorCode) : Exception()