package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter

sealed class PhotosActivityEvent : BaseEvent {
    class StartUploadingService : PhotosActivityEvent()
    class StartReceivingService : PhotosActivityEvent()
    class FailedToUploadPhotoButtonClick(val clickType: UploadedPhotosAdapter.UploadedPhotosAdapterButtonClick) : PhotosActivityEvent()
}