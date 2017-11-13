package com.kirakishou.photoexchange.helper.service.wires.inputs

import com.kirakishou.photoexchange.mvvm.model.other.LonLat

/**
 * Created by kirakishou on 11/4/2017.
 */
interface UploadPhotoServiceInputs {
    fun uploadPhoto(photoFilePath: String, location: LonLat, userId: String)
}