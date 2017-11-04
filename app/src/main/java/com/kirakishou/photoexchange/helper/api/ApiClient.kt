package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.mvvm.model.dto.PhotoWithInfo
import com.kirakishou.photoexchange.mvvm.model.net.response.SendPhotoResponse
import com.kirakishou.photoexchange.mvvm.model.net.response.StatusResponse
import io.reactivex.Single

/**
 * Created by kirakishou on 7/23/2017.
 */
interface ApiClient {
    fun sendPhoto(info: PhotoWithInfo): Single<SendPhotoResponse>
}