package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.CachedPhotoIdEntity
import com.kirakishou.photoexchange.helper.database.repository.CachedPhotoIdRepository
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single

open class RestoreAccountUseCase(
    private val apiClient: ApiClient,
    private val database: MyDatabase,
    private val cachedPhotoIdRepository: CachedPhotoIdRepository,
    private val settingsRepository: SettingsRepository,
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val receivedPhotosRepository: ReceivedPhotosRepository
) {
    fun restoreAccount(oldUserId: String): Single<Either<ErrorCode.CheckAccountExistsErrors, Boolean>> {
        return apiClient.checkAccountExists(oldUserId)
            .flatMap { response ->
                val errorCode = response.errorCode as ErrorCode.CheckAccountExistsErrors
                if (errorCode !is ErrorCode.CheckAccountExistsErrors.Ok) {
                    return@flatMap Single.just(Either.Error(errorCode))
                }

                if (!response.accountExists) {
                    return@flatMap Single.just(Either.Value(false))
                }

                val transactionResult = database.transactional {
                    if (!settingsRepository.saveUserId(oldUserId)) {
                        return@transactional false
                    }

                    uploadedPhotosRepository.deleteAll()
                    receivedPhotosRepository.deleteAll()
                    cachedPhotoIdRepository.deleteAll(CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
                    cachedPhotoIdRepository.deleteAll(CachedPhotoIdEntity.PhotoType.UploadedPhoto)

                    return@transactional true
                }

                if (!transactionResult) {
                    return@flatMap Single.just(Either.Error(ErrorCode.CheckAccountExistsErrors.LocalDatabaseError()))
                }

                return@flatMap Single.just(Either.Value(true))
            }
    }
}