package com.kirakishou.photoexchange.mvvm.model.event

import com.kirakishou.photoexchange.mvvm.model.dto.PhotoNameWithId
import kotlin.reflect.KClass

/**
 * Created by kirakishou on 11/7/2017.
 */
class SendPhotoEvent(
        val type: EventType,
        val status: SendPhotoEventStatus,
        val response: PhotoNameWithId?,
        owner: KClass<*>

) : BaseEvent(owner)

enum class EventType {
    UploadPhoto
}