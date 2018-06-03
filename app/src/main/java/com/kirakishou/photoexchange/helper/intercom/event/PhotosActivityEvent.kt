package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter

sealed class PhotosActivityEvent : BaseEvent {
    class StartUploadingService(val callerClass: Class<*>,
                                val reason: String) : PhotosActivityEvent()
    class StartReceivingService(val callerClass: Class<*>,
                                val reason: String) : PhotosActivityEvent()
    class FailedToUploadPhotoButtonClick(val clickType: UploadedPhotosAdapter.UploadedPhotosAdapterButtonClick) : PhotosActivityEvent()
}