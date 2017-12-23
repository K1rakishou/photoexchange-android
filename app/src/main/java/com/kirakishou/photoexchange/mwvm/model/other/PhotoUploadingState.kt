package com.kirakishou.photoexchange.mwvm.model.other

/**
 * Created by kirakishou on 12/23/2017.
 */
sealed class PhotoUploadingState {
    class NoPhotoToUpload : PhotoUploadingState()
    class StartPhotoUploading : PhotoUploadingState()
    class PhotoUploaded(val photo: TakenPhoto) : PhotoUploadingState()
    class FailedToUploadPhoto(val photo: TakenPhoto) : PhotoUploadingState()
    class AllPhotosUploaded : PhotoUploadingState()
    class UnknownErrorWhileUploading(val error: Throwable) : PhotoUploadingState()
}
