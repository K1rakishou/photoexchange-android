package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class UseCaseResult<R> {
    class Result<R>(val result: R): UseCaseResult<R>()
    class Error(val errorCode: ErrorCode) : UseCaseResult<ErrorCode>()
}