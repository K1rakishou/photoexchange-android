package com.kirakishou.photoexchange.mvvm.model.dto

import com.kirakishou.photoexchange.mvvm.model.LonLat
import java.io.File

/**
 * Created by kirakishou on 11/3/2017.
 */
class PhotoWithInfo(val id: Long,
                    val photoFilePath: String,
                    val location: LonLat,
                    val userId: String)