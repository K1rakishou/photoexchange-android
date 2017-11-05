package com.kirakishou.photoexchange.helper.service.wires.inputs

import com.kirakishou.photoexchange.mvvm.model.LonLat
import java.io.File

/**
 * Created by kirakishou on 11/4/2017.
 */
interface SendPhotoServiceInputs {
    fun uploadPhoto(photoFilePath: String, location: LonLat, userId: String)
}