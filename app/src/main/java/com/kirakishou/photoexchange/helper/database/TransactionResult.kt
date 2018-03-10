package com.kirakishou.photoexchange.helper.database

/**
 * Created by kirakishou on 3/10/2018.
 */
sealed class TransactionResult<T> {
    class Fail<T>(val result: T) : TransactionResult<T>()
    class Success<T>(val result: T): TransactionResult<T>()
}