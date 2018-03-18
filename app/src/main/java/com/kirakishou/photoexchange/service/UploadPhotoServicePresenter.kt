package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.extension.asWeak
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import timber.log.Timber

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoServicePresenter(
    private val photosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val coroutinePool: CoroutineThreadPoolProvider
) {
    private val tag = "[${this::class.java.simpleName}] "

    private var uploadingJob: Deferred<Unit>? = null
    private var serviceCallbacks: UploadPhotoServiceCallbacks? = null

    fun onAttach(serviceCallbacks: UploadPhotoServiceCallbacks) {
        this.serviceCallbacks = serviceCallbacks
    }

    fun onDetach() {
        this.serviceCallbacks = null
        this.uploadingJob?.cancel()
    }

    fun uploadPhotos() {
        uploadingJob = async(coroutinePool.BG()) {
            val userId = settingsRepository.findUserId()
            val location = settingsRepository.findLastLocation()

            if (userId != null && location != null)  {
                photosRepository.uploadPhotos(userId, location, serviceCallbacks?.asWeak())
            } else {
                Timber.tag(tag).e("Either userId or location is null! userId = $userId, location = $location")
            }

            serviceCallbacks?.stopService()
            return@async Unit
        }
    }
}