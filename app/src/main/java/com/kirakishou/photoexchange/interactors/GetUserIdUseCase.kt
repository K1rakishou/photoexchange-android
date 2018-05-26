package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.rx2.rxSingle
import timber.log.Timber

class GetUserIdUseCase(
    private val settingsRepository: SettingsRepository,
    private val apiClient: ApiClient
) {
    private val TAG = "GetUserIdUseCase"

    fun getUserId(): Single<Either<ErrorCode, String>> {
        return rxSingle {
            try {
                val userId = settingsRepository.getUserId()
                if (userId.isNotEmpty()) {
                    return@rxSingle Either.Value(userId)
                }

                val response = try {
                    apiClient.getUserId().await()
                } catch (error: RuntimeException) {
                    if (error.cause == null && error.cause !is InterruptedException) {
                        throw error
                    }

                    return@rxSingle Either.Error(ErrorCode.UploadPhotoErrors.LocalInterrupted())
                }

                val errorCode = response.errorCode
                if (errorCode !is ErrorCode.GetUserIdError.Ok) {
                    return@rxSingle Either.Error(ErrorCode.UploadPhotoErrors.LocalCouldNotGetUserId())
                }

                if (!settingsRepository.saveUserId(response.userId)) {
                    return@rxSingle Either.Error(ErrorCode.UploadPhotoErrors.LocalDatabaseError())
                }

                return@rxSingle Either.Value(response.userId)
            } catch (error: Throwable) {
                Timber.tag(TAG).e(error)
                return@rxSingle Either.Error(ErrorCode.GetUserIdError.UnknownError())
            }
        }
    }
}