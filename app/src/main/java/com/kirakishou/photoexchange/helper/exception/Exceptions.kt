package com.kirakishou.photoexchange.helper.exception

import core.ErrorCode

class EmptyUserIdException : Exception("Empty UserId. You should upload at least one photo first.")
class ApiErrorException(val errorCode: ErrorCode) : Exception("Request status is not OK. Error code is: $errorCode, errorMessage is: ${errorCode.getErrorMessage()}")
class BadServerResponse(statusCode: Int) : Exception("Server returned bad response, statusCode = ${statusCode}")
class ConnectionError(message: String?) : Exception(message)
class DatabaseException(message: String?) : Exception(message)
class FirebaseException(message: String?) : Exception(message)