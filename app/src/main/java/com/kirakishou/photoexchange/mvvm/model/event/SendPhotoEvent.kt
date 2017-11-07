package com.kirakishou.photoexchange.mvvm.model.event

/**
 * Created by kirakishou on 11/7/2017.
 */
class SendPhotoEvent(
        val status: SendPhotoEventStatus,
        val photoName: String

) : BaseEvent()