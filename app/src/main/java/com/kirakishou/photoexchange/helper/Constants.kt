package com.kirakishou.photoexchange.helper

import com.kirakishou.fixmypc.photoexchange.BuildConfig
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 11/8/2017.
 */
object Constants {
  val isDebugBuild = BuildConfig.DEBUG
  const val appid = "com.kirakishou.photoexchange"
  const val BASE_URL = "http://kez1911.asuscomm.com:8080/"
  const val BASE_PHOTOS_URL = "${BASE_URL}v1/api/get_photo"
  const val BASE_STATIC_MAP_URL = "${BASE_URL}v1/api/get_static_map"
  const val DATABASE_NAME = "photoexchange_db"
  const val DOMAIN_NAME = "photoexchange.io"

  const val DEFAULT_ADAPTER_ITEM_WIDTH = 288
  const val DEFAULT_PHOTOS_PER_PAGE_COUNT = 5

  const val DELIMITER = ","

  val INSERTED_EARLIER_THAN_TIME_DELTA = TimeUnit.MINUTES.toMillis(1)
}