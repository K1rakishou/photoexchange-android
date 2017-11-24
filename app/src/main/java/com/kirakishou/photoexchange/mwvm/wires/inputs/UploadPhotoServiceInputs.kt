package com.kirakishou.photoexchange.mwvm.wires.inputs

import com.kirakishou.photoexchange.mwvm.model.other.LonLat

/**
 * Created by kirakishou on 11/4/2017.
 */
interface UploadPhotoServiceInputs {
    fun uploadPhoto(id: Long, photoFilePath: String, location: LonLat, userId: String)
}