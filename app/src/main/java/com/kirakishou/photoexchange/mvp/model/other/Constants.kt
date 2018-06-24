package com.kirakishou.photoexchange.mvp.model.other

import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.helper.extension.minutes

/**
 * Created by kirakishou on 11/8/2017.
 */
object Constants {
    val isDebugBuild = BuildConfig.DEBUG
    val appid = "com.kirakishou.photoexchange"
    val SHARED_PREFS_PREFIX = "${appid}_SHARED_PREF"
    val BASE_URL = "http://kez1911.asuscomm.com:8080/"
    val DATABASE_NAME = "photoexchange_db"

    val PHOTOS_DELIMITER = ","

    val GALLERY_PHOTOS_PER_ROW = 15
    val UPLOADED_PHOTOS_PER_ROW = 15
    val RECEIVED_PHOTOS_PER_ROW = 15

    val DEFAULT_ADAPTER_ITEM_WIDTH = 288

    //interval to update photos in the db with fresh information
    val INTERVAL_TO_REFRESH_PHOTOS_FROM_SERVER = 30.minutes()
}