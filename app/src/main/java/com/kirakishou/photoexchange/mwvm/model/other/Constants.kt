package com.kirakishou.photoexchange.mwvm.model.other

import com.kirakishou.fixmypc.photoexchange.BuildConfig

/**
 * Created by kirakishou on 11/8/2017.
 */
object Constants {
    val isDebugBuild = BuildConfig.DEBUG
    val appid = "com.kirakishou.photoexchange"

    val NOTIFICATION_ID = 1
    val CHANNEL_ID = "$appid.UPLOAD_PHOTO_CHANNEL_ID"
    val CHANNEL_NAME = "$appid.UPLOAD_PHOTO_CHANNEL_NAME"
}