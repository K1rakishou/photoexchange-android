package com.kirakishou.photoexchange.mvvm.model.dto

import com.kirakishou.photoexchange.mvvm.model.other.LonLat

/**
 * Created by kirakishou on 11/3/2017.
 */
class PhotoToBeUploaded(val photoFilePath: String,
                        val location: LonLat,
                        val userId: String)