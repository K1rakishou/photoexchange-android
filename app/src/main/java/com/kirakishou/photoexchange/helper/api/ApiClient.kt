package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.mvvm.model.dto.PhotoToBeUploaded
import com.kirakishou.photoexchange.mvvm.model.net.response.UploadPhotoResponse
import io.reactivex.Single

/**
 * Created by kirakishou on 7/23/2017.
 */
interface ApiClient {
    fun sendPhoto(info: PhotoToBeUploaded): Single<UploadPhotoResponse>
}