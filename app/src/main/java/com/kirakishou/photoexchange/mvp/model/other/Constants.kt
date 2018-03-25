package com.kirakishou.photoexchange.mvp.model.other

import com.kirakishou.fixmypc.photoexchange.BuildConfig

/**
 * Created by kirakishou on 11/8/2017.
 */
object Constants {
    val isDebugBuild = BuildConfig.DEBUG
    val appid = "com.kirakishou.photoexchange"
    val SHARED_PREFS_PREFIX = "${appid}_SHARED_PREF"
    val BASE_URL = "http://kez1911.asuscomm.com:8080/"
    val DATABASE_NAME = "photoexchange_db"
}