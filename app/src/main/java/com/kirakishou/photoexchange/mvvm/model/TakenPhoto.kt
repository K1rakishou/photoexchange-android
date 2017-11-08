package com.kirakishou.photoexchange.mvvm.model

import android.arch.persistence.room.ColumnInfo

/**
 * Created by kirakishou on 11/8/2017.
 */
data class TakenPhoto(
        var lon: Double,
        var lat: Double,
        var userId: String,
        var photoName: String,
        var photoFilePath: String,
        var wasSent: Boolean
)