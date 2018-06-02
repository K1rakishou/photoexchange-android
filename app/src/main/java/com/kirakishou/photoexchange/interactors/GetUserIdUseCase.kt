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

    fun getUserId(): Single<Either<ErrorCode.GetUserIdError, String>> {
        return Single.fromCallable { settingsRepository.getUserId() }
            .flatMap { userId ->
                if (userId.isNotEmpty()) {
                    return@flatMap Single.just(Either.Value(userId))
                }

                return@flatMap apiClient.getUserId()
                    .map { response ->
                        val errorCode = response.errorCode as ErrorCode.GetUserIdError
                        if (errorCode !is ErrorCode.GetUserIdError.Ok) {
                            return@map Either.Error(errorCode)
                        }

                        if (!settingsRepository.saveUserId(response.userId)) {
                            return@map Either.Error(ErrorCode.GetUserIdError.LocalDatabaseError())
                        }

                        return@map Either.Value(response.userId)
                    }
            }
            .onErrorReturn { error ->
                Timber.tag(TAG).e(error)
                return@onErrorReturn Either.Error(ErrorCode.GetUserIdError.UnknownError())
            }
    }
}