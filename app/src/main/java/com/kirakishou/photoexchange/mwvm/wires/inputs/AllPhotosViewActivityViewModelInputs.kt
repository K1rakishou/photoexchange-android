package com.kirakishou.photoexchange.mwvm.wires.inputs

import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.model.state.LookingForPhotoState
import com.kirakishou.photoexchange.mwvm.model.state.PhotoUploadingState

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelInputs {
    fun getPhotoListUserNewLocations(photos: List<TakenPhoto>)
    fun beginReceivingEvents(clazz: Class<*>)
    fun stopReceivingEvents(clazz: Class<*>)
    fun fetchOnePageUploadedPhotos(page: Int, count: Int)
    fun fetchOnePageReceivedPhotos(page: Int, count: Int)
    fun scrollToTop()
    fun showLookingForPhotoIndicator()
    fun getQueuedUpAndFailedToUploadPhotos()
    fun cancelTakenPhotoUploading(id: Long)
    fun startLookingForPhotos()
    fun startPhotosUploading()
    fun deletePhotoAnswerFromDatabase(photoName: String)

    fun updatePhotoUploadingState(receiver: Class<*>, newState: PhotoUploadingState)
    fun updateLookingForPhotoState(receiver: Class<*>, newState: LookingForPhotoState)
}