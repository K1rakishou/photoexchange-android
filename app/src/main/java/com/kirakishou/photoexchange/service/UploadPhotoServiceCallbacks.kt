package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Single

/**
 * Created by kirakishou on 3/17/2018.
 */
interface UploadPhotoServiceCallbacks {
    fun getCurrentLocation(): Single<LonLat>
}