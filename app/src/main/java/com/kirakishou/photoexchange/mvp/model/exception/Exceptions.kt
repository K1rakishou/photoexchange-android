package com.kirakishou.photoexchange.mvp.model.exception

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class EmptyUserIdException : Exception()
class ApiErrorException(val errorCode: ErrorCode) : Exception()
class BadServerResponse : Exception()
class ConnectionError(message: String?) : Exception(message)
class UnknownException(val exception: Exception) : Exception()
class DatabaseException(message: String) : Exception(message)