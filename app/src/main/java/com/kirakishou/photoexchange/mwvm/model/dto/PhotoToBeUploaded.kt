package com.kirakishou.photoexchange.mwvm.model.dto

import com.kirakishou.photoexchange.mwvm.model.other.LonLat

/**
 * Created by kirakishou on 11/3/2017.
 */
class PhotoToBeUploaded(val photoFilePath: String,
                        val location: LonLat,
                        val userId: String)