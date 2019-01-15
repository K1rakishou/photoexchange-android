package com.kirakishou.photoexchange.helper.exception

import core.ErrorCode

class EmptyUserUuidException
  : Exception("Empty UserUuid. You should upload at least one photo first.")
class ApiErrorException(val errorCode: ErrorCode)
  : Exception("Request status is not OK. Error code is: $errorCode, errorMessage is: ${errorCode.getErrorMessage()}")
class BadServerResponse(statusCode: Int)
  : Exception("Server returned bad response, statusCode = ${statusCode}")
class NetworkAccessDisabledInSettings
  : Exception("Current network is metered and it is disabled in the settings to access internet with metered network. You can change this behavior in the settings.")
class ImageLoadingDisabledInSettings
  : Exception("Current network is metered and it is disabled in the settings to load images from the internet with metered network. You can change this behavior in the settings.")
class AttemptToLoadImagesWithMeteredNetworkException(methodName: String)
  : Exception("Attempt to load images with metered network connection from ${methodName} method")
class AttemptToAccessInternetWithMeteredNetworkException(methodName: String)
  : Exception("Attempt to access internet with metered network connection from ${methodName} method")

class ConnectionError(message: String?) : Exception(message)
class DatabaseException(message: String?) : Exception(message)
class FirebaseException(message: String?) : Exception(message)