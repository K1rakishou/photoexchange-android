package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.asSingle
import kotlinx.coroutines.experimental.rx2.await

class GetUserIdUseCase(
    private val settingsRepository: SettingsRepository,
    private val apiClient: ApiClient
) {

    fun getUserId(): Single<Either<ErrorCode, String>> {
        return async {
            try {
                val userId = settingsRepository.getUserId()
                if (userId.isNotEmpty()) {
                    return@async Either.Value(userId)
                }

                val response = try {
                    apiClient.getUserId().await()
                } catch (error: RuntimeException) {
                    if (error.cause == null && error.cause !is InterruptedException) {
                        throw error
                    }

                    return@async Either.Error(ErrorCode.UploadPhotoErrors.Local.Interrupted())
                }

                val errorCode = response.errorCode
                if (errorCode !is ErrorCode.GetUserIdError.Remote.Ok) {
                    return@async Either.Error(ErrorCode.UploadPhotoErrors.Remote.CouldNotGetUserId())
                }

                if (!settingsRepository.saveUserId(response.userId)) {
                    return@async Either.Error(ErrorCode.UploadPhotoErrors.Local.DatabaseError())
                }

                return@async Either.Value(response.userId)
            } catch (error: Throwable) {
                return@async Either.Error(ErrorCode.GetUserIdError.Remote.UnknownError())
            }
        }.asSingle(CommonPool)
    }
}