package com.kirakishou.photoexchange.mwvm.model.other

/**
 * Created by kirakishou on 12/23/2017.
 */
sealed class PhotoUploadingStatus {
    class NoPhotoToUpload : PhotoUploadingStatus()
    class StartPhotoUploading : PhotoUploadingStatus()
    class PhotoUploaded(val photo: TakenPhoto) : PhotoUploadingStatus()
    class FailedToUploadPhoto(val photo: TakenPhoto) : PhotoUploadingStatus()
    class AllPhotosUploaded : PhotoUploadingStatus()
    class UnknownErrorWhileUploading(val error: Throwable) : PhotoUploadingStatus()
}
