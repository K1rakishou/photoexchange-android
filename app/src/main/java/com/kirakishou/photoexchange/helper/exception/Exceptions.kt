package com.kirakishou.photoexchange.helper.exception

import core.ErrorCode

class EmptyUserIdException : Exception("Empty UserId. You should upload at least one photo first.")
class ApiErrorException(val errorCode: ErrorCode) : Exception("Request status is not OK. Error code is: $errorCode, errorMessage is: ${errorCode.getErrorMessage()}")
class BadServerResponse : Exception("Server returned bad response.")
class ConnectionError(message: String?) : Exception(message)
class DatabaseException(message: String?) : Exception(message)