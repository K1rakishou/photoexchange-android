package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.mvvm.model.dto.PhotoWithLocation
import com.kirakishou.photoexchange.mvvm.model.net.response.SendPhotoResponse
import com.kirakishou.photoexchange.mvvm.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mvvm.viewmodel.MainActivityViewModel
import io.reactivex.Single

/**
 * Created by kirakishou on 7/23/2017.
 */
interface ApiClient {
    fun <T : StatusResponse> sendPhoto(info: PhotoWithLocation): Single<T>
}