package com.kirakishou.photoexchange.mwvm.wires.inputs

import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelInputs {
    fun fetchOnePageUploadedPhotos(page: Int, count: Int)
    fun fetchOnePageReceivedPhotos(page: Int, count: Int)
    fun scrollToTop()
    fun showLookingForPhotoIndicator()
    fun startUploadingPhotos()
    fun photoUploaded(photo: TakenPhoto)
    fun showFailedToUploadPhoto(photo: TakenPhoto)
    fun showPhotoReceived(photo: PhotoAnswer, allFound: Boolean)
    fun showErrorWhileTryingToLookForPhoto()
    fun showNoPhotoOnServer()
    fun showUserNeedsToUploadMorePhotos()
    fun shouldStartLookingForPhotos()
    fun allPhotosUploaded()
    fun getQueuedUpPhotos()
    fun cancelTakenPhotoUploading(id: Long)
}