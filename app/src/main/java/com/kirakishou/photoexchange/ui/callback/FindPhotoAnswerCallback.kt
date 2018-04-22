package com.kirakishou.photoexchange.ui.callback

import com.kirakishou.photoexchange.mvp.model.PhotoFindEvent

interface FindPhotoAnswerCallback {
    fun onPhotoFindEvent(event: PhotoFindEvent)
}