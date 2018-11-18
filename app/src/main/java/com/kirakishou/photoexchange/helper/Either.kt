package com.kirakishou.photoexchange.helper

import java.lang.Exception

sealed class Either<out E, out V> {
  data class Error<out E>(val error: E) : Either<E, Nothing>()
  data class Value<out V>(val value: V) : Either<Nothing, V>()
}

suspend fun <E : Exception, T> myRunCatching(block: suspend () -> T): Either<E, T> {
  return try {
    Either.Value(block())
  } catch (error: Exception) {
    Either.Error(error as E)
  }
}