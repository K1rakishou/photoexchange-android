package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.extension.asWeak
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
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

    private var uploadingJob: Job? = null
    private var serviceCallbacks: UploadPhotoServiceCallbacks? = null

    fun onAttach(serviceCallbacks: UploadPhotoServiceCallbacks) {
        this.serviceCallbacks = serviceCallbacks
    }

    fun onDetach() {
        this.serviceCallbacks = null
        this.uploadingJob?.cancel()
    }

    fun uploadPhotos() {
        val weakenCallback = serviceCallbacks?.asWeak()

        uploadingJob = launch(coroutinePool.BG()) {
            val userId = settingsRepository.findUserId()
            val location = settingsRepository.findLastLocation()

            if (userId != null && location != null)  {
                photosRepository.uploadPhotos(userId, location, weakenCallback)
            } else {
                Timber.tag(tag).e("Either userId or location is null! userId = $userId, location = $location")
            }

            serviceCallbacks?.stopService()
            return@launch Unit
        }
    }
}