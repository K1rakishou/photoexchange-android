package com.kirakishou.photoexchange.mvp.model.exception

import com.kirakishou.photoexchange.mvp.model.other.ServerErrorCode


/**
 * Created by kirakishou on 8/25/2017.
 */
class ApiException(val serverErrorCode: ServerErrorCode) : Exception()