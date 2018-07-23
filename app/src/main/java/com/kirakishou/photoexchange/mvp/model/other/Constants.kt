package com.kirakishou.photoexchange.mvp.model.other

import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.helper.extension.minutes

/**
 * Created by kirakishou on 11/8/2017.
 */
object Constants {
    val isDebugBuild = BuildConfig.DEBUG
    const val appid = "com.kirakishou.photoexchange"
    val SHARED_PREFS_PREFIX = "${appid}_SHARED_PREF"
    const val BASE_URL = "http://kez1911.asuscomm.com:8080/"
    const val DATABASE_NAME = "photoexchange_db"

    const val PHOTOS_DELIMITER = ","

    const  val GALLERY_PHOTOS_PER_ROW = 15
    const val UPLOADED_PHOTOS_PER_ROW = 15
    const val RECEIVED_PHOTOS_PER_ROW = 15

    const val DEFAULT_ADAPTER_ITEM_WIDTH = 288

    const val ADAPTER_LOAD_MORE_ITEMS_DELAY_MS = 800L
    const val PROGRESS_FOOTER_REMOVE_DELAY_MS = 200L

    //interval to update photos in the db with fresh information
    val GALLERY_PHOTOS_INFO_CACHE_MAX_LIVE_TIME = 30.minutes()
    val UPLOADED_PHOTOS_CACHE_MAX_LIVE_TIME = 30.minutes()
    val RECEIVED_PHOTOS_CACHE_MAX_LIVE_TIME = 30.minutes()
}